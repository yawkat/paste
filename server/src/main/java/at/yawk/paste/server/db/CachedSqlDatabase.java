package at.yawk.paste.server.db;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.server.Config;
import at.yawk.paste.server.PasteIdSpecification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.postgresql.PGStatement;

@Singleton
public class CachedSqlDatabase implements Database {
    private static final StatementCustomizer PREPARE_IMMEDIATELY = new StatementCustomizer() {
        @Override
        public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
            stmt.unwrap(PGStatement.class).setPrepareThreshold(-1);
        }
    };

    private final Cache<String, Optional<Paste>> pasteCache = CacheBuilder.newBuilder()
            .softValues().expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    @Inject PasteIdSpecification pasteIdSpecification;

    private final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory())
            .findAndRegisterModules();

    private Jdbi dbi;

    @Inject
    void connect(Config config) {
        Properties properties = new Properties();
        properties.putAll(config.getSqlConfig());
        HikariDataSource dataSource = new HikariDataSource(new HikariConfig(properties));

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();

        dbi = Jdbi.create(dataSource);
    }

    @Nullable
    @Override
    public Optional<Paste> getPaste(String id, boolean block) {
        return getPaste0(id, block, true);
    }

    /**
     * @param cache Whether we may save data to the cache.
     */
    @SuppressWarnings("OptionalAssignedToNull")
    private Optional<Paste> getPaste0(String id, boolean block, boolean cache) {
        Optional<Paste> cached = pasteCache.getIfPresent(id);
        if (cached == null && block) {
            cached = dbi.withHandle(handle -> handle.createQuery("select id, time, data from paste where id = ?")
                    .bind(0, id)
                    .addCustomizer(PREPARE_IMMEDIATELY)
                    .map(this::map)
                    .findOne());
            if (cache) {
                pasteCache.put(id, cached);
            }
        }
        return cached;
    }

    @Override
    public Paste insertPaste(PasteData data) {
        Paste paste = new Paste();
        paste.setTime(Instant.now());
        paste.setData(data);

        Random rng = ThreadLocalRandom.current();
        do {
            // generate an ID
            paste.setId(pasteIdSpecification.generate(rng));
        } while (!insertPaste(paste));
        return paste;
    }

    /**
     * @return {@code false} iff the id is already in use
     */
    boolean insertPaste(Paste paste) {
        byte[] bytes;
        try {
            bytes = objectMapper.writerFor(PasteData.class).writeValueAsBytes(paste.getData());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
        return dbi.withHandle(handle -> {
            int n = handle.createUpdate("insert into paste (id, time, data) values (?, ?, ?) on conflict do nothing ")
                    .addCustomizer(PREPARE_IMMEDIATELY)
                    .bind(0, paste.getId())
                    .bind(1, paste.getTime())
                    .bind(2, bytes)
                    .execute();
            if (n == 0) {
                handle.rollback();
                return false;
            } else {
                if (n != 1) { throw new AssertionError(); }
                return true;
            }
        });
    }

    private Paste map(ResultSet rs, StatementContext ctx) throws SQLException {
        Paste paste = new Paste();
        paste.setId(rs.getString("id"));
        LocalDateTime ldt = rs.getObject("time", LocalDateTime.class);
        paste.setTime(ldt == null ? null : ldt.toInstant(ZoneOffset.UTC));
        try {
            paste.setData(objectMapper.readValue(rs.getBytes("data"), PasteData.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return paste;
    }
}
