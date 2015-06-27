package at.yawk.paste.client;

import at.yawk.paste.model.PasteData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.*;
import java.util.Base64;
import lombok.SneakyThrows;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 * @author yawkat
 */
public class PasteClient {
    private static final int CHUNK_SIZE = 4096;

    private final Config config;
    private final ObjectMapper jsonObjectMapper;
    private final ObjectMapper msgpackObjectMapper;

    private KeyPair keyPair;

    public PasteClient(Config config, ObjectMapper jsonObjectMapper) {
        this.config = config;
        this.jsonObjectMapper = jsonObjectMapper;
        this.msgpackObjectMapper = new ObjectMapper(new MessagePackFactory());
        this.msgpackObjectMapper.findAndRegisterModules();
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    private KeyPair getKeyPair() throws IOException {
        if (keyPair == null) {
            synchronized (this) {
                if (Files.exists(config.getKeyFile())) {
                    try (InputStream in = Files.newInputStream(config.getKeyFile())) {
                        keyPair = jsonObjectMapper.readValue(in, RsaKeyPair.class).toKeyPair();
                    } catch (GeneralSecurityException e) {
                        throw new IOException(e);
                    }
                } else {
                    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                    generator.initialize(4096);
                    keyPair = generator.generateKeyPair();
                    try (OutputStream out = Files.newOutputStream(config.getKeyFile())) {
                        jsonObjectMapper.writeValue(out, RsaKeyPair.ofKeyPair(keyPair));
                    }
                }
            }
        }
        return keyPair;
    }

    public String save(PasteData data) throws IOException {
        return save(data, UploadProgressListener.NOOP);
    }

    /**
     * Save the given paste data.
     *
     * @return The full URL where this paste was saved.
     */
    public String save(PasteData data, UploadProgressListener progressListener) throws IOException {
        byte[] bytes = msgpackObjectMapper.writeValueAsBytes(data);
        progressListener.update(0, bytes.length);

        URL url = config.getRemote();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        KeyPair keyPair = getKeyPair();

        // build authorization header

        // Authorization: Signature <public key base64> <signature base64>
        StringBuilder authHeaderBuilder = new StringBuilder("Signature ");
        // public key
        authHeaderBuilder.append(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        authHeaderBuilder.append(' ');
        // signature
        try {
            Signature signature = Signature.getInstance("SHA512withRSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(bytes);
            authHeaderBuilder.append(Base64.getEncoder().encodeToString(signature.sign()));
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }

        connection.setRequestProperty("Authorization", authHeaderBuilder.toString());

        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);

        connection.setFixedLengthStreamingMode(bytes.length);

        int off = 0;
        while (off < bytes.length) {
            connection.getOutputStream().write(bytes, off, Math.min(bytes.length - off, CHUNK_SIZE));
            off += CHUNK_SIZE;
            progressListener.update(off, bytes.length);
        }
        connection.getOutputStream().close();

        int responseCode = connection.getResponseCode();
        if (responseCode >= 300) {
            connection.disconnect();
            throw new IOException("Status: " + responseCode);
        }

        String targetLocation = connection.getHeaderField("Location");

        connection.disconnect();

        URL remote = config.getRemote();
        return remote.getPath().endsWith("/") ? remote + targetLocation : remote + "/" + targetLocation;
    }
}
