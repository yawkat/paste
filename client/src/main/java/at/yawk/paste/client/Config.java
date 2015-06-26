package at.yawk.paste.client;

import at.yawk.paste.model.ImageFormat;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Data;
import lombok.SneakyThrows;

/**
 * @author yawkat
 */
@Data
public class Config {
    private Path keyFile = Paths.get("key");
    private URL remote = defaultUrl();
    private ImageFormat preferredImageFormat = ImageFormat.PNG;

    @SneakyThrows(MalformedURLException.class)
    private static URL defaultUrl() {
        return new URL("http://127.0.0.1:8080");
    }
}
