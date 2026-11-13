package at.yawk.paste.server;

import at.yawk.paste.server.db.Database;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpServerExchange;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author yawkat
 */
class Server {
    private final Config config;
    private final TemplateEngine templateEngine;
    private final Database database;
    private final List<Servlet> servlets;

    Server(Config config, TemplateEngine templateEngine, Database database, List<Servlet> servlets) {
        this.config = config;
        this.templateEngine = templateEngine;
        this.database = database;
        List<Servlet> sorted = new ArrayList<>(servlets);
        sorted.sort(Comparator.comparingInt(s -> {
            Servlet.Priority priority = s.getClass().getAnnotation(Servlet.Priority.class);
            return priority == null ? 0 : priority.value();
        }));
        this.servlets = List.copyOf(sorted);
    }

    void handle(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            // Servlets and the database may block, so every request runs on Undertow's worker pool. Its bounded size
            // also limits how many requests (and request bodies) are in memory at once.
            exchange.dispatch(this::handle);
            return;
        }
        new Request(exchange, database, templateEngine, servlets.iterator()).proceed();
    }

    void start() {
        Undertow undertow = Undertow.builder()
                .addHttpListener(config.getPort(), config.getHost())
                .setHandler(this::handle)
                .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true)
                .build();

        undertow.start();
    }
}
