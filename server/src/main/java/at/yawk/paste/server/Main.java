package at.yawk.paste.server;

import at.yawk.paste.server.servlet.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.Arrays;

/**
 * @author yawkat
 */
public class Main {
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(
                new ConfigProvider()
        );
        Server server = injector.getInstance(Server.class);
        server.setServlets(Arrays.asList(
                injector.getInstance(ImageDisplayServlet.class),
                injector.getInstance(NotFoundServlet.class),
                injector.getInstance(RawImageServlet.class),
                injector.getInstance(RawSVGServlet.class),
                injector.getInstance(RawVideoServlet.class),
                injector.getInstance(SVGDisplayServlet.class),
                injector.getInstance(TextDisplayServlet.class),
                injector.getInstance(TwitterCardImageServlet.class),
                injector.getInstance(UploadServlet.class),
                injector.getInstance(URLForwardServlet.class),
                injector.getInstance(VideoDisplayServlet.class)
        ));
        server.start();
    }
}
