package at.yawk.paste.server;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class Config {
    private String host = "127.0.0.1";
    private int port = 8080;
    private Path keyLocation = Paths.get("key.pub");
    private Map<String, String> sqlConfig;
}
