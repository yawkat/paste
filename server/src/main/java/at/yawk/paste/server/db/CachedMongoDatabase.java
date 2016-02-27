package at.yawk.paste.server.db;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.server.Config;
import at.yawk.paste.server.PasteIdSpecification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mongodb.DB;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.mongojack.JacksonDBCollection;

/**
 * @author yawkat
 */
@Singleton
class CachedMongoDatabase implements Database {
    private final Cache<String, Optional<Paste>> pasteCache = CacheBuilder.newBuilder()
            .softValues().expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    @Inject PasteIdSpecification pasteIdSpecification;

    private JacksonDBCollection<Paste, String> collection;

    @Inject
    void connect(Config config, ObjectMapper objectMapper) {
        MongoConfig mongoConfig = config.getMongoConfig();
        MongoClient client = new MongoClient(mongoConfig.getServers());

        @SuppressWarnings("deprecation")
        DB db = client.getDB(mongoConfig.getDatabase());
        collection = JacksonDBCollection.wrap(
                db.getCollection(mongoConfig.getCollection()),
                Paste.class,
                String.class,
                objectMapper
        );
    }

    @Nullable
    @Override
    public Optional<Paste> getPaste(String id, boolean block) {
        return getPaste0(id, block, true);
    }

    /**
     * @param cache Whether we may save data to the cache.
     */
    private Optional<Paste> getPaste0(String id, boolean block, boolean cache) {
        Optional<Paste> cached = pasteCache.getIfPresent(id);
        if (cached == null && block) {
            Paste item = collection.findOneById(id);
            cached = Optional.ofNullable(item);
            if (cache) {
                pasteCache.put(id, cached);
            }
        }
        return cached;
    }

    @Override
    public Paste insertPaste(PasteData data) {
        Paste paste = new Paste();
        paste.setData(data);

        Random rng = ThreadLocalRandom.current();
        while (true) {
            // generate an ID
            paste.setId(pasteIdSpecification.generate(rng));

            try {
                collection.insert(paste, WriteConcern.ACKNOWLEDGED);
                break;
            } catch (DuplicateKeyException e) {
                // duplicate key, retry
            }
        }
        return paste;
    }

    void upsert(Paste paste) {
        collection.removeById(paste.getId());
        collection.insert(paste);
    }
}
