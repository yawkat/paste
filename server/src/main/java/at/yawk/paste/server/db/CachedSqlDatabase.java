package at.yawk.paste.server.db;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.server.Config;
import at.yawk.paste.server.MsgpackReader;
import at.yawk.paste.server.PasteIdSpecification;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.postgresql.PGStatement;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.cbor.CBORMapper;

public class CachedSqlDatabase implements Database {
    private static final int DATA_VERSION_MSGPACK = 1;
    private static final int DATA_VERSION_CBOR = 2;

    private static final StatementCustomizer PREPARE_IMMEDIATELY = new StatementCustomizer() {
        @Override
        public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
            stmt.unwrap(PGStatement.class).setPrepareThreshold(-1);
        }
    };

    private final Cache<String, Optional<Paste>> pasteCache = Caffeine.newBuilder()
            .softValues().expireAfterWrite(Duration.ofMinutes(1))
            .build();

    private final ObjectMapper cborMapper = CBORMapper.builder().build();

    private final PasteIdSpecification pasteIdSpecification;
    private final Jdbi dbi;

    public CachedSqlDatabase(Config config, PasteIdSpecification pasteIdSpecification) {
        this.pasteIdSpecification = pasteIdSpecification;

        Properties properties = new Properties();
        properties.putAll(config.getSqlConfig());
        HikariDataSource dataSource = new HikariDataSource(new HikariConfig(properties));

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();

        dbi = Jdbi.create(dataSource);
    }

    @Override
    public Optional<Paste> getPaste(String id) {
        // not Cache.get(id, loader): that would hold a lock shared with other ids during the query
        Optional<Paste> cached = pasteCache.getIfPresent(id);
        if (cached == null) {
            cached = loadPaste(id);
            pasteCache.put(id, cached);
        }
        return cached;
    }

    private Optional<Paste> loadPaste(String id) {
        return dbi.withHandle(handle -> handle.createQuery("select id, time, data, data_version from paste where id = ?")
                .bind(0, id)
                .addCustomizer(PREPARE_IMMEDIATELY)
                .map(this::map)
                .findOne());
    }

    @Override
    public Paste insertPaste(PasteData data) {
        Instant time = Instant.now();
        RandomGenerator rng = ThreadLocalRandom.current();
        Paste paste;
        do {
            // generate an ID
            paste = new Paste(pasteIdSpecification.generate(rng), time, data);
        } while (!insertPaste(paste));
        return paste;
    }

    /**
     * @return {@code false} iff the id is already in use
     */
    boolean insertPaste(Paste paste) {
        byte[] bytes = cborMapper.writerFor(PasteData.class).writeValueAsBytes(paste.data());
        return dbi.withHandle(handle -> {
            int n = handle.createUpdate("insert into paste (id, time, data, data_version) values (?, ?, ?, ?) on conflict do nothing ")
                    .addCustomizer(PREPARE_IMMEDIATELY)
                    .bind(0, paste.id())
                    .bind(1, paste.time())
                    .bind(2, bytes)
                    .bind(3, DATA_VERSION_CBOR)
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
        LocalDateTime ldt = rs.getObject("time", LocalDateTime.class);
        PasteData data;
        try {
            data = deserializePasteData(rs.getBytes("data"), rs.getInt("data_version"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Paste(rs.getString("id"), ldt == null ? null : ldt.toInstant(ZoneOffset.UTC), data);
    }

    /**
     * Deserialize paste data based on the stored data version.
     * Version 1 = msgpack, Version 2 = CBOR.
     */
    private PasteData deserializePasteData(byte[] data, int dataVersion) throws IOException {
        return switch (dataVersion) {
            case DATA_VERSION_MSGPACK -> cborMapper.treeToValue(MsgpackReader.read(data), PasteData.class);
            case DATA_VERSION_CBOR -> cborMapper.readValue(data, PasteData.class);
            default -> throw new IOException("Unknown data version: " + dataVersion);
        };
    }
}
