package at.yawk.paste.server.servlet;

import at.yawk.paste.model.ImagePasteData;

/**
 * @author yawkat
 */
public class ImageDisplayServlet extends DisplayServlet<ImagePasteData> {
    public ImageDisplayServlet() {
        super("image");
    }
}
