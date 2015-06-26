package at.yawk.paste.server.servlet;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.model.TextPasteData;
import at.yawk.paste.server.PasteServlet;
import at.yawk.paste.server.Request;
import at.yawk.yarn.Component;
import java.util.List;
import lombok.Value;

/**
 * @author yawkat
 */
@Component
@PasteServlet.Suffix("")
public class TextDisplayServlet extends PasteServlet<TextPasteData> {
    @Override
    protected void handle(Request request, Paste paste, TextPasteData data, List<String> groups) throws Exception {
        request.render("text", paste.withData(new SplittedText(data.getText().split("\n"))));
    }

    @Value
    public static class SplittedText implements PasteData {
        String[] lines;
    }
}
