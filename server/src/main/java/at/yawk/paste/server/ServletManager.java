package at.yawk.paste.server;

import at.yawk.paste.server.db.Database;
import at.yawk.yarn.Component;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import java.util.Comparator;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * @author yawkat
 */
@Component
class ServletManager {
    @Inject List<Servlet> servlets;
    @Inject Config config;
    @Inject Database database;

    @PostConstruct
    void sortServlets() {
        servlets.sort(Comparator.<Servlet>comparingInt(s -> {
            Servlet.Priority priority = s.getClass().getAnnotation(Servlet.Priority.class);
            return priority == null ? 0 : priority.value();
        }));
    }

    void handle(HttpServerExchange exchange) {
        new Request(exchange, database, servlets.iterator()).proceed();
    }

    @PostConstruct
    void start() {
        Undertow undertow = Undertow.builder()
                .addHttpListener(config.getPort(), config.getHost())
                .setHandler(this::handle)
                .build();

        undertow.start();
    }
}
