package at.yawk.paste.server.servlet;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.VideoPasteData;
import at.yawk.paste.server.PasteServlet;
import at.yawk.paste.server.Request;
import io.undertow.util.HttpString;
import java.util.List;

/**
 * @author yawkat
 */
@PasteServlet.Suffix("\\.webm")
public class RawVideoServlet extends PasteServlet<VideoPasteData> {
    @Override
    protected void handle(Request request, Paste paste, VideoPasteData data, List<String> groups) throws Exception {
        request.getExchange().setResponseCode(200);
        request.getExchange().getResponseHeaders().add(new HttpString("Content-Type"), "video/webm");

        byte[] bytes = data.getVideo();
        request.setContentLength(bytes.length);
        request.getOutputStream().write(bytes);

        request.finish();
    }
}
