package at.yawk.paste.model;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@Getter
@RequiredArgsConstructor
public enum ImageFormat {
    PNG("png", List.of("png"), "image/png") {
        @Override
        public BufferedImage decode(InputStream source) throws IOException {
            return ImageIO.read(source);
        }

        @Override
        public void encode(RenderedImage image, OutputStream target) throws IOException {
            ImageIO.write(image, "PNG", target);
        }
    },
    JPEG("jpg", List.of("jpeg", "jpg"), "image/jpeg") {
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

    public abstract BufferedImage decode(InputStream source) throws IOException;

    public abstract void encode(RenderedImage image, OutputStream target) throws IOException;

    public void transformTo(InputStream source, ImageFormat targetFormat, OutputStream target) throws IOException {
        targetFormat.encode(decode(source), target);
    }

    // lookup

    private static final Map<String, ImageFormat> EXTENSIONS = Arrays.stream(values())
            .flatMap(format -> format.getExtensions().stream().map(extension -> Map.entry(extension, format)))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    @Nullable
    public static ImageFormat byExtension(String ext) {
        return EXTENSIONS.get(ext);
    }
}
