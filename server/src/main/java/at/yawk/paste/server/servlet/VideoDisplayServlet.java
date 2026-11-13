package at.yawk.paste.server.servlet;

import at.yawk.paste.model.VideoPasteData;

/**
 * @author yawkat
 */
public class VideoDisplayServlet extends DisplayServlet<VideoPasteData> {
    public VideoDisplayServlet(Context context) {
        super("video", context);
    }
}
