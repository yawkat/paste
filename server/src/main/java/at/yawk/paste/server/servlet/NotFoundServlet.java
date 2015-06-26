package at.yawk.paste.server.servlet;

import at.yawk.paste.server.Request;
import at.yawk.paste.server.Servlet;
import at.yawk.yarn.Component;

/**
 * @author yawkat
 */
@Component
@Servlet.Priority(Integer.MAX_VALUE) // run last
public class NotFoundServlet implements Servlet {
    @Override
    public void handle(Request request) {
        request.render("404", null);
    }
}
