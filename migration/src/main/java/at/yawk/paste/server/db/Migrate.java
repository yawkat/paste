package at.yawk.paste.server.db;

import at.yawk.paste.model.Paste;
import at.yawk.paste.server.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.io.File;
import java.io.IOException;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

public class Migrate {
    public static void main(String[] args) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
        MongoConfig mongoConfig = args.length >= 2 ? yamlMapper.readValue(new File(args[1]), MongoConfig.class) :
                new MongoConfig();

        MongoDatabase db = MongoClients.create(mongoConfig.getConnectionString())
                .getDatabase(mongoConfig.getDatabase());
        JacksonMongoCollection<Paste> collection = JacksonMongoCollection.builder()
                .withObjectMapper(new ObjectMapper()
                                          .findAndRegisterModules()
                                          .registerModule(new FloatInstantModule()))
                .build(db, mongoConfig.getCollection(), Paste.class, UuidRepresentation.JAVA_LEGACY);

        CachedSqlDatabase sqlDatabase = new CachedSqlDatabase();
        sqlDatabase.connect(yamlMapper.readValue(new File(args[0]), Config.class));
        for (Paste paste : collection.find()) {
            boolean inserted = sqlDatabase.insertPaste(paste);
            if (!inserted) {
                throw new RuntimeException();
            }
        }
    }
}
