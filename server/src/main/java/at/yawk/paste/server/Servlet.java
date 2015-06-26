package at.yawk.paste.server;

import io.undertow.server.HttpServerExchange;
import java.lang.annotation.*;

/**
 * @author yawkat
 */
public interface Servlet {
    void handle(HttpServerExchange request);

    /**
     * Priority of this servlet. Natural ordering is applied; lower priority values are applied first.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Priority {
        int value();
    }
}
