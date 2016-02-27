package at.yawk.paste.server;

import at.yawk.paste.server.db.Database;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpServerExchange;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;

/**
 * @author yawkat
 */
class Server {
    @Inject Config config;
    @Inject TemplateEngine templateEngine;
    @Inject Database database;

    private List<Servlet> servlets;

    void setServlets(List<Servlet> servlets) {
        this.servlets = servlets;
        servlets.sort(Comparator.<Servlet>comparingInt(s -> {
            Servlet.Priority priority = s.getClass().getAnnotation(Servlet.Priority.class);
            return priority == null ? 0 : priority.value();
        }));
    }

    void handle(HttpServerExchange exchange) throws Exception {
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
