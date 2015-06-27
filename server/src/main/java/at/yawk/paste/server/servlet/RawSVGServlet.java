package at.yawk.paste.server.servlet;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.SVGPasteData;
import at.yawk.paste.server.PasteServlet;
import at.yawk.paste.server.Request;
import at.yawk.yarn.Component;
import io.undertow.util.HttpString;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author yawkat
 */
@Component
@PasteServlet.Suffix("\\.svg")
public class RawSVGServlet extends PasteServlet<SVGPasteData> {
    @Override
    protected void handle(Request request, Paste paste, SVGPasteData data, List<String> groups) throws Exception {
        request.getExchange().setResponseCode(200);
        request.getExchange().getResponseHeaders().add(new HttpString("Content-Type"), "image/svg+xml");

        byte[] bytes = data.getSvg().getBytes(StandardCharsets.UTF_8);
        request.setContentLength(bytes.length);
        request.getOutputStream().write(bytes);

        request.finish();
    }
}
