package at.yawk.paste.server;

import at.yawk.paste.model.Paste;
import at.yawk.paste.server.db.Database;
import io.undertow.server.HttpServerExchange;
import java.util.Iterator;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Request {
    @Getter private final HttpServerExchange exchange;
    private final Database database;
    private final Iterator<Servlet> servletIterator;

    private CacheEntry pasteCache = null;

    @Nullable
    public Optional<Paste> getPaste(String id, boolean block) {
        if (pasteCache != null && pasteCache.id.equals(id)) {
            return pasteCache.getPaste();
        } else {
            Optional<Paste> paste = database.getPaste(id, block);
            if (paste != null) {
                pasteCache = new CacheEntry(id, paste);
            }
            return paste;
        }
    }

    public void proceed() {
        if (servletIterator.hasNext()) {
            servletIterator.next().handle(this);
        }
    }

    public void finish() {
        exchange.endExchange();
    }

    @Value
    private static class CacheEntry {
        String id;
        Optional<Paste> paste;
    }
}
