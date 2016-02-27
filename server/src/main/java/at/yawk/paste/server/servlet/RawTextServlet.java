package at.yawk.paste.server.servlet;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.TextPasteData;
import at.yawk.paste.server.PasteServlet;
import at.yawk.paste.server.Request;
import io.undertow.util.HttpString;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author yawkat
 */
@PasteServlet.Suffix("\\.txt")
public class RawTextServlet extends PasteServlet<TextPasteData> {
    @Override
    protected void handle(Request request, Paste paste, TextPasteData data, List<String> groups) throws Exception {
        request.getExchange().setResponseCode(200);
        request.getExchange().getResponseHeaders().add(new HttpString("Content-Type"), "text/plain; charset=utf-8");

        byte[] bytes = data.getText().getBytes(StandardCharsets.UTF_8);
        request.setContentLength(bytes.length);
        request.getOutputStream().write(bytes);

        request.finish();
    }
}
