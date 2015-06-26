package at.yawk.paste.server;

import java.lang.annotation.*;

/**
 * @author yawkat
 */
public interface Servlet {
    void handle(Request request) throws Exception;

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
