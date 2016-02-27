package at.yawk.paste.server;

import at.yawk.paste.model.Paste;
import at.yawk.paste.server.db.Database;
import io.undertow.server.HttpServerExchange;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Request {
    @Getter private final HttpServerExchange exchange;
    @Getter private final Database database;
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

    public void proceed() throws Exception {
        if (servletIterator.hasNext()) {
            servletIterator.next().handle(this);
        } else {
            log.warn("{} reached end of servlet pipeline", this);
        }
    }

    @SneakyThrows
    public void render(String viewName, Object model) {
        Writer writer = new OutputStreamWriter(getOutputStream());
        templateEngine.render(viewName, writer, model);
        finish();
    }

    public void setContentLength(long contentLength) {
        exchange.setResponseContentLength(contentLength);
    }

    public InputStream getInputStream() {
        if (!exchange.isBlocking()) {
            exchange.startBlocking();
        }
        return exchange.getInputStream();
    }

    public OutputStream getOutputStream() {
        if (!exchange.isBlocking()) {
            exchange.startBlocking();
        }
        return exchange.getOutputStream();
    }

    public Optional<String> getParameter(String key) {
        Deque<String> parameters = exchange.getQueryParameters().get(key);
        return parameters == null || parameters.isEmpty() ? Optional.empty() : Optional.of(parameters.peek());
    }

    public void finish() {
        exchange.endExchange();
        if (log.isInfoEnabled()) {
            long requestStartTime = exchange.getRequestStartTime();
            if (requestStartTime == -1) {
                log.info("{}", this);
            } else {
                log.info("{} : {} ns", this, String.format("%9d", System.nanoTime() - requestStartTime));
            }
        }
    }

    @Value
    private static class CacheEntry {
        String id;
        Optional<Paste> paste;
    }

    @Override
    public String toString() {
        return "[" + exchange.getSourceAddress() + " - " + exchange.getRequestMethod() + " " +
               exchange.getRequestURI() + "]";
    }
}
