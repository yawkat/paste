package at.yawk.paste.server.servlet;

import at.yawk.paste.model.SVGPasteData;

/**
 * @author yawkat
 */
public class SVGDisplayServlet extends DisplayServlet<SVGPasteData> {
    public SVGDisplayServlet() {
        super("svg");
    }
}
