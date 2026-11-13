package at.yawk.paste.server.servlet;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.TextPasteData;
import at.yawk.paste.server.PasteServlet;
import at.yawk.paste.server.Request;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author yawkat
 */
@PasteServlet.Suffix("")
public class TextDisplayServlet extends PasteServlet<TextPasteData> {
    public TextDisplayServlet(Context context) {
        super(context);
    }

    @Override
    protected void handle(Request request, Paste paste, TextPasteData data, List<String> groups) throws Exception {
        request.render("text", new View(paste.id(), new SplittedText(
                request.getParameter("highlight")
                        // sanitize
                        .map(lang -> lang.replaceAll("[^a-zA-Z0-9\\-_+]", ""))
                        .orElse(null),
                data.text().split("\n")
        )));
    }

    /**
     * Template model, in the shape of a {@link Paste}.
     */
    public record View(String id, SplittedText data) {}

    public record SplittedText(@Nullable String highlightPreference, String[] lines) {}
}
