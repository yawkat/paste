package at.yawk.paste.server.servlet;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.model.TextPasteData;
import at.yawk.paste.server.PasteServlet;
import at.yawk.paste.server.Request;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * @author yawkat
 */
@PasteServlet.Suffix("")
public class TextDisplayServlet extends PasteServlet<TextPasteData> {
    @Override
    protected void handle(Request request, Paste paste, TextPasteData data, List<String> groups) throws Exception {
        request.render("text", paste.withData(new SplittedText(
                request.getParameter("highlight")
                        // saniztize
                        .map(lang -> lang.replaceAll("[^a-zA-Z0-9\\-_+]", ""))
                        .orElse(null),
                data.getText().split("\n")
        )));
    }

    @Value
    public static class SplittedText implements PasteData {
        @Nullable String highlightPreference;
        String[] lines;
    }
}
