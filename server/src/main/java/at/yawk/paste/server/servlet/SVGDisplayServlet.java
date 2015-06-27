package at.yawk.paste.server.servlet;

import at.yawk.paste.model.SVGPasteData;
import at.yawk.yarn.Component;

/**
 * @author yawkat
 */
@Component
public class SVGDisplayServlet extends DisplayServlet<SVGPasteData> {
    public SVGDisplayServlet() {
        super("svg");
    }
}
