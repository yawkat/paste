package at.yawk.paste.client;

import at.yawk.paste.model.ImageFormat;
import at.yawk.paste.model.ImagePasteData;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.model.TextPasteData;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.mozilla.intl.chardet.nsDetector;
import sun.awt.image.ToolkitImage;

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
            if (flavor.equals(DataFlavor.imageFlavor)) {
                Image image = (Image) transferable.getTransferData(DataFlavor.imageFlavor);
                return getImagePasteData(image);
            }
            if (flavor.equals(DataFlavor.stringFlavor)) {
                return getTextPasteData((String) transferable.getTransferData(DataFlavor.stringFlavor));
            }
            if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                //noinspection unchecked
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (files.size() == 1) {
                    return getFilePasteData(files.get(0));
                }
            }
        }
        return null;
    }

    public PasteData getFilePasteData(File file) throws IOException {
        if (!file.exists() || file.isDirectory()) { return null; }

        try {
            ImagePasteData imageData = getImagePasteData(file);
            if (imageData != null) {
                return imageData;
            }
        } catch (IOException ignored) {}

        byte[] bytes = Files.readAllBytes(file.toPath());

        nsDetector charsetDetector = new nsDetector();

        AtomicReference<String> charsetName = new AtomicReference<>();
        charsetDetector.Init(name -> charsetName.compareAndSet(null, name));

        if (!charsetDetector.isAscii(bytes, bytes.length)) {
            charsetDetector.DoIt(bytes, bytes.length, false);
        }

        Charset charset = StandardCharsets.US_ASCII;
        if (charsetName.get() != null) {
            try {
                charset = Charset.forName(charsetName.get());
            } catch (UnsupportedCharsetException ignored) {}
        }

        return getTextPasteData(new String(bytes, charset));
    }

    public TextPasteData getTextPasteData(String text) {
        TextPasteData data = new TextPasteData();
        data.setText(text);
        return data;
    }

    public ImagePasteData getImagePasteData(File file) throws IOException {
        ImageFormat supportedFormat = null;
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(input);
            while (imageReaders.hasNext() && supportedFormat == null) {
                String formatName = imageReaders.next().getFormatName();
                switch (formatName.toLowerCase()) {
                case "png":
                    supportedFormat = ImageFormat.PNG;
                    break;
                case "jpeg":
                    supportedFormat = ImageFormat.JPEG;
                    break;
                }
            }
        }

        if (supportedFormat == null) {
            BufferedImage image = ImageIO.read(file);
            return image == null ? null : getImagePasteData(image);
        } else {
            ImagePasteData data = new ImagePasteData();
            data.setFormat(supportedFormat);
            data.setData(Files.readAllBytes(file.toPath()));
            return data;
        }
    }

    public ImagePasteData getImagePasteData(Image image) throws IOException {
        RenderedImage renderedImage;
        if (image instanceof RenderedImage) {
            renderedImage = (RenderedImage) image;
        } else if (image instanceof RenderableImage) {
            renderedImage = ((RenderableImage) image).createDefaultRendering();
        } else if (image instanceof VolatileImage) {
            renderedImage = ((VolatileImage) image).getSnapshot();
        } else if (image instanceof ToolkitImage) {
            renderedImage = ((ToolkitImage) image).getBufferedImage();
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
            renderedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D graphics = ((BufferedImage) renderedImage).createGraphics();
            graphics.drawImage(image, null, null);
            graphics.dispose();
        }

        ImageFormat format = config.getPreferredImageFormat();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        format.encode(renderedImage, buf);

        ImagePasteData data = new ImagePasteData();
        data.setFormat(format);
        data.setData(buf.toByteArray());
        return data;
    }
}
