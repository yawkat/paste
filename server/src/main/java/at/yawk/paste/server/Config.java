package at.yawk.paste.server;

import at.yawk.paste.server.db.MongoConfig;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class Config {
    private String host = "127.0.0.1";
    private int port = 8080;
    private MongoConfig mongoConfig = new MongoConfig();
    private Path keyLocation = Paths.get("key.pub");
}
