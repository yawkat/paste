package at.yawk.paste.server.servlet;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.server.PasteServlet;
import at.yawk.paste.server.Request;
import java.util.List;

/**
 * @author yawkat
 */
@PasteServlet.Suffix("")
public abstract class DisplayServlet<D extends PasteData> extends PasteServlet<D> {
    private final String viewName;

    public DisplayServlet(String viewName) {
        this.viewName = viewName;
    }

    @Override
    protected void handle(Request request, Paste paste, D data, List<String> groups) {
        request.render(viewName, paste);
    }
}
