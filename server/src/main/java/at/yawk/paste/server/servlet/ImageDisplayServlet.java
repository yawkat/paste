package at.yawk.paste.server.servlet;

import at.yawk.paste.model.ImagePasteData;
import at.yawk.yarn.Component;

/**
 * @author yawkat
 */
@Component
public class ImageDisplayServlet extends DisplayServlet<ImagePasteData> {
    public ImageDisplayServlet() {
        super("image");
    }
}
