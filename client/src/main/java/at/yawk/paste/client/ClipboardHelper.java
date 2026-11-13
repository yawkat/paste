package at.yawk.paste.client;

import at.yawk.paste.model.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.VolatileImage;
import java.awt.image.renderable.RenderableImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.mozilla.universalchardet.UniversalDetector;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class ClipboardHelper {
    private final Config config;

    @Nullable
    public PasteData getCurrentClipboardData() throws IOException {
        return getPasteData(Toolkit.getDefaultToolkit().getSystemClipboard());
    }

    @Nullable
    public PasteData getPasteData(Clipboard clipboard) throws IOException {
        return getPasteData(clipboard.getContents(null));
    }

    @Nullable
    @SneakyThrows(UnsupportedFlavorException.class) // checked with getTransferDataFlavors
    public PasteData getPasteData(Transferable transferable) throws IOException {
        for (DataFlavor flavor : transferable.getTransferDataFlavors()) {
            if (flavor.equals(DataFlavor.imageFlavor)
                    && transferable.getTransferData(DataFlavor.imageFlavor) instanceof Image image) {
                return getImagePasteData(image);
            }
            if (flavor.equals(DataFlavor.stringFlavor)
                    && transferable.getTransferData(DataFlavor.stringFlavor) instanceof String text) {
                URLPasteData urlPasteData = getUrlPasteData(text);
                if (urlPasteData != null) { return urlPasteData; }
                return getTextPasteData(text);
            }
            if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                //noinspection unchecked
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (files.size() == 1) {
                    return getFilePasteData(files.getFirst());
                }
            }
        }
        return null;
    }

    @Nullable
    public PasteData getFilePasteData(File file) throws IOException {
        if (!file.exists() || file.isDirectory()) { return null; }

        try {
            ImagePasteData imageData = getImagePasteData(file);
            if (imageData != null) {
                return imageData;
            }
        } catch (IOException ignored) {}

        return getTextPasteData(Files.readAllBytes(file.toPath()));
    }

    /**
     * Decode the given bytes as text, detecting the charset. Falls back to UTF-8, which also covers plain ASCII.
     */
    public TextPasteData getTextPasteData(byte[] bytes) {
        UniversalDetector charsetDetector = new UniversalDetector();
        charsetDetector.handleData(bytes, 0, bytes.length);
        charsetDetector.dataEnd();
        String charsetName = charsetDetector.getDetectedCharset();

        Charset charset = StandardCharsets.UTF_8;
        if (charsetName != null) {
            try {
                charset = Charset.forName(charsetName);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException ignored) {}
        }

        return getTextPasteData(new String(bytes, charset));
    }

    public TextPasteData getTextPasteData(String text) {
        return new TextPasteData(text);
    }

    @Nullable
    public URLPasteData getUrlPasteData(String url) {
        try {
            // toURL rejects relative URIs and unknown protocols
            return new URLPasteData(new URI(url).toURL());
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public ImagePasteData getImagePasteData(File file) throws IOException {
        ImageFormat supportedFormat = null;
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(input);
            while (imageReaders.hasNext() && supportedFormat == null) {
                String formatName = imageReaders.next().getFormatName();
                supportedFormat = switch (formatName.toLowerCase(Locale.ROOT)) {
                    case "png" -> ImageFormat.PNG;
                    case "jpeg" -> ImageFormat.JPEG;
                    default -> null;
                };
            }
        }

        if (supportedFormat == null) {
            BufferedImage image = ImageIO.read(file);
            return image == null ? null : getImagePasteData(image);
        } else {
            return getImagePasteData(Files.readAllBytes(file.toPath()), supportedFormat);
        }
    }

    /**
     * Create image paste data from bytes that are already encoded in the given format.
     */
    public ImagePasteData getImagePasteData(byte[] bytes, ImageFormat format) {
        return new ImagePasteData(bytes, format);
    }

    public ImagePasteData getImagePasteData(Image image) throws IOException {
        RenderedImage renderedImage;
        if (image instanceof RenderedImage rendered) {
            renderedImage = rendered;
        } else if (image instanceof RenderableImage renderable) {
            renderedImage = renderable.createDefaultRendering();
        } else if (image instanceof VolatileImage volatileImage) {
            renderedImage = volatileImage.getSnapshot();
        } else {
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            if (width == -1 || height == -1) {
                class WaitingObserver implements ImageObserver {
                    boolean complete = false;

                    @Override
                    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                        if (infoflags == ALLBITS) {
                            synchronized (this) {
                                complete = true;
                                notify();
                            }
                        }
                        return false;
                    }

                    public void waitFor() throws InterruptedException {
                        synchronized (this) {
                            while (!complete) {
                                wait();
                            }
                        }
                    }
                }
                WaitingObserver observer = new WaitingObserver();
                image.getWidth(observer);
                image.getHeight(observer);

                try {
                    observer.waitFor();
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }

                width = image.getWidth(null);
                height = image.getHeight(null);
            }
            BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D graphics = buffered.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            renderedImage = buffered;
        }

        ImageFormat format = config.getPreferredImageFormat();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        format.encode(renderedImage, buf);

        return new ImagePasteData(buf.toByteArray(), format);
    }
}
