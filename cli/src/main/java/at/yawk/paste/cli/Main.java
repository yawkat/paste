package at.yawk.paste.cli;

import at.yawk.paste.client.ClipboardHelper;
import at.yawk.paste.client.Config;
import at.yawk.paste.client.PasteClient;
import at.yawk.paste.model.ImageFormat;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.model.URLPasteData;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Command line paste client.
 *
 * <ul>
 *     <li>{@code paste}: upload stdin (text, URL, PNG or JPEG) and print the URL</li>
 *     <li>{@code paste file <path>}: upload a file, copy the URL to the clipboard and show a notification. This is
 *     meant to be launched from a desktop file, e.g. from Spectacle's "Open With" menu.</li>
 * </ul>
 *
 * @author yawkat
 */
public class Main {
    private static final byte[] PNG_MAGIC = { (byte) 0x89, 'P', 'N', 'G' };
    private static final byte[] JPEG_MAGIC = { (byte) 0xff, (byte) 0xd8, (byte) 0xff };

    private final Config config;
    private final ClipboardHelper helper;
    private final PasteClient client;

    private Main(Config config, ObjectMapper jsonMapper) {
        this.config = config;
        this.helper = new ClipboardHelper(config);
        this.client = new PasteClient(config, jsonMapper);
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        boolean fileMode = args.length > 0 && args[0].equals("file");
        if (args.length > 0 && !fileMode) {
            System.err.println("Usage: paste < input\n       paste file <path>");
            System.exit(2);
        }

        try {
            ObjectMapper jsonMapper = JsonMapper.builder()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();
            Main main = new Main(loadConfig(jsonMapper), jsonMapper);
            if (fileMode) {
                main.uploadFiles(Arrays.copyOfRange(args, 1, args.length));
            } else {
                main.uploadStdin();
            }
        } catch (Exception e) {
            if (fileMode) {
                notify("Paste upload failed", String.valueOf(e.getMessage()));
            }
            System.err.println("Upload failed: " + e);
            System.exit(1);
        }
    }

    private static Path configDirectory() {
        String xdg = System.getenv("XDG_CONFIG_HOME");
        Path base = xdg == null || xdg.isEmpty() ? Path.of(System.getProperty("user.home"), ".config") : Path.of(xdg);
        return base.resolve("paste");
    }

    private static Config loadConfig(ObjectMapper jsonMapper) throws IOException {
        Path directory = configDirectory();
        Path file = directory.resolve("config.json");
        Config config = Files.exists(file) ? jsonMapper.readValue(file.toFile(), Config.class) : new Config();
        // relative paths are relative to the config directory
        config.setKeyFile(directory.resolve(config.getKeyFile()));
        if (!Files.exists(config.getKeyFile())) {
            // the server only accepts the first key it has seen, so never silently generate a new one
            throw new IOException("Key file " + config.getKeyFile() + " does not exist");
        }
        return config;
    }

    private void uploadStdin() throws IOException {
        byte[] bytes = System.in.readAllBytes();
        if (bytes.length == 0) {
            throw new IOException("No input");
        }
        System.out.println(client.save(fromBytes(bytes)));
    }

    private PasteData fromBytes(byte[] bytes) {
        if (startsWith(bytes, PNG_MAGIC)) {
            return helper.getImagePasteData(bytes, ImageFormat.PNG);
        }
        if (startsWith(bytes, JPEG_MAGIC)) {
            return helper.getImagePasteData(bytes, ImageFormat.JPEG);
        }
        String text = helper.getTextPasteData(bytes).getText();
        String trimmed = text.trim();
        // only a single token counts as a URL, so that e.g. "note: something" stays text
        if (trimmed.matches("[a-zA-Z][a-zA-Z0-9+.-]*://\\S+")) {
            URLPasteData url = helper.getUrlPasteData(trimmed);
            if (url != null) {
                return url;
            }
        }
        return helper.getTextPasteData(text);
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
    }

    private void uploadFiles(String[] paths) throws IOException {
        if (paths.length != 1) {
            throw new IOException("Expected exactly one file");
        }
        PasteData data = helper.getFilePasteData(new File(paths[0]));
        if (data == null) {
            throw new IOException("Not a file: " + paths[0]);
        }
        String url = client.save(data);
        System.out.println(url);

        String body = url;
        try {
            copyToClipboard(url);
        } catch (IOException e) {
            body += "\n(could not copy to clipboard: " + e.getMessage() + ")";
        }
        if ("open".equals(notify("Paste uploaded", body, "--action=open=Open"))) {
            new ProcessBuilder("xdg-open", url).inheritIO().start();
        }
    }

    private static void copyToClipboard(String text) throws IOException {
        // wl-copy forks into the background to serve the clipboard, so don't keep its output pipes open
        Process process = new ProcessBuilder("wl-copy")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        try (OutputStream out = process.getOutputStream()) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        try {
            if (process.waitFor() != 0) {
                throw new IOException("wl-copy exited with " + process.exitValue());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    /**
     * Show a desktop notification. If actions are given, this blocks until the notification is closed.
     *
     * @return The action that was invoked, or {@code null}
     */
    @Nullable
    private static String notify(String summary, String body, String... extraArgs) {
        String[] command = new String[4 + extraArgs.length];
        command[0] = "notify-send";
        command[1] = "--app-name=Paste";
        System.arraycopy(extraArgs, 0, command, 2, extraArgs.length);
        command[2 + extraArgs.length] = summary;
        command[3 + extraArgs.length] = body;
        try {
            Process process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            String action;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                action = reader.readLine();
            }
            process.waitFor();
            return action;
        } catch (IOException e) {
            System.err.println("Failed to send notification: " + e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
