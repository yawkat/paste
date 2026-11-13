package at.yawk.paste.client;

import at.yawk.paste.model.ImageFormat;
import java.net.URI;
import java.nio.file.Path;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class Config {
    private Path keyFile = Path.of("key");
    private URI remote = URI.create("http://127.0.0.1:8080");
    private ImageFormat preferredImageFormat = ImageFormat.PNG;
}
