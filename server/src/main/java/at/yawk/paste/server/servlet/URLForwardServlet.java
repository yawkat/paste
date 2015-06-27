package at.yawk.paste.server.servlet;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.URLPasteData;
import at.yawk.paste.server.PasteServlet;
import at.yawk.paste.server.Request;
import at.yawk.yarn.Component;
import io.undertow.util.HttpString;
import java.util.List;

/**
 * @author yawkat
 */
@Component
@PasteServlet.Suffix("")
public class URLForwardServlet extends PasteServlet<URLPasteData> {
    @Override
    protected void handle(Request request, Paste paste, URLPasteData data, List<String> groups) throws Exception {
        request.getExchange().setResponseCode(301);
        request.getExchange().getResponseHeaders().add(new HttpString("Location"), data.getUrl().toString());
    }
}
