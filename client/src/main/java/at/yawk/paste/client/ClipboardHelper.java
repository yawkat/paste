package at.yawk.paste.client;

import at.yawk.paste.model.ImageFormat;
import at.yawk.paste.model.ImagePasteData;
import at.yawk.paste.model.PasteData;
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
import java.io.IOException;
import java.io.InterruptedIOException;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
        }
        return null;
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
