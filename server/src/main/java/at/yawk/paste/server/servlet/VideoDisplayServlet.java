package at.yawk.paste.server.servlet;

import at.yawk.paste.model.VideoPasteData;
import at.yawk.yarn.Component;

/**
 * @author yawkat
 */
@Component
public class VideoDisplayServlet extends DisplayServlet<VideoPasteData> {
    public VideoDisplayServlet() {
        super("video");
    }
}
