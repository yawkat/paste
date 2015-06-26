package at.yawk.paste.server;

import at.yawk.paste.model.Paste;
import at.yawk.paste.server.db.Database;
import io.undertow.server.HttpServerExchange;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.*;

/**
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Request {
    @Getter private final HttpServerExchange exchange;
    private final Database database;
    private final TemplateEngine templateEngine;
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

    @SneakyThrows
    public void render(String viewName, Object model) {
        exchange.startBlocking();
        Writer writer = new OutputStreamWriter(exchange.getOutputStream());
        templateEngine.render(viewName, writer, model);
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
