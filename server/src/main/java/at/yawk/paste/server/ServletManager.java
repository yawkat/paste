package at.yawk.paste.server;

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

    @PostConstruct
    void sortServlets() {
        servlets.sort(Comparator.<Servlet>comparingInt(s -> {
            Servlet.Priority priority = s.getClass().getAnnotation(Servlet.Priority.class);
            return priority == null ? 0 : priority.value();
        }));
    }

    void handle(HttpServerExchange exchange) {
        for (Servlet servlet : servlets) {
            servlet.handle(exchange);
            if (exchange.isComplete() || exchange.isBlocking()) {
                // return since we would error below
                return;
            }
        }
        exchange.setResponseCode(404);
        exchange.endExchange();
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
