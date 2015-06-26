package at.yawk.paste.model;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@Getter
@RequiredArgsConstructor
public enum ImageFormat {
    PNG("png", Collections.singleton("png"), "image/png") {
        @Override
        public BufferedImage decode(InputStream source) throws IOException {
            return ImageIO.read(source);
        }

        @Override
        public void encode(RenderedImage image, OutputStream target) throws IOException {
            ImageIO.write(image, "PNG", target);
        }
    },
    JPEG("jpg", Arrays.asList("jpeg", "jpg"), "image/jpeg") {
        @Override
        public BufferedImage decode(InputStream source) throws IOException {
            return ImageIO.read(source);
        }

        @Override
        public void encode(RenderedImage image, OutputStream target) throws IOException {
            ImageIO.write(image, "PNG", target);
        }
    };

    private final String defaultExtension;
    private final Collection<String> extensions;
    private final String mediaType;

    public abstract RenderedImage decode(InputStream source) throws IOException;

    public abstract void encode(RenderedImage image, OutputStream target) throws IOException;

    public void transformTo(InputStream source, ImageFormat targetFormat, OutputStream target) throws IOException {
        targetFormat.encode(decode(source), target);
    }

    // lookup

    private static final Map<String, ImageFormat> EXTENSIONS = new HashMap<String, ImageFormat>() {{
        for (ImageFormat format : ImageFormat.values()) {
            for (String extension : format.getExtensions()) {
                put(extension, format);
            }
        }
    }};

    @Nullable
    public static ImageFormat byExtension(String ext) {
        return EXTENSIONS.get(ext);
    }
}
