package at.yawk.paste.server.db;

import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class MongoConfig {
    private String connectionString = "mongodb://localhost";
    private String database = "paste";
    private String collection = "paste";
}
