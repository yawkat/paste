package at.yawk.paste.server.db;

import com.mongodb.ServerAddress;
import java.util.Collections;
import java.util.List;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class MongoConfig {
    private List<ServerAddress> servers =
            Collections.singletonList(new ServerAddress(ServerAddress.defaultHost(), ServerAddress.defaultPort()));
    private String database = "paste";
    private String collection = "paste";
}
