package at.yawk.paste.server.servlet;

import at.yawk.paste.model.ImageFormat;
import at.yawk.paste.model.ImagePasteData;
import at.yawk.paste.server.Request;
import at.yawk.yarn.Component;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author yawkat
 */
@Component
public class TwitterCardImageServlet extends RawImageServlet {
    @Override
    protected String getSuffixPattern() {
        return "\\.twitter" + super.getSuffixPattern();
    }

    @Override
    protected void dumpImage(Request request, ImagePasteData data, ImageFormat expectedFormat) throws IOException {
        BufferedImage image = data.getFormat().decode(new ByteArrayInputStream(data.getData()));
        if (image.getWidth() < 280 || image.getHeight() < 150) {
            BufferedImage resized = new BufferedImage(
                    Math.max(image.getWidth(), 280), Math.max(image.getWidth(), 150), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D graphics = resized.createGraphics();
            graphics.setBackground(Color.WHITE);
            graphics.clearRect(0, 0, resized.getWidth(), resized.getHeight());
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            image = resized;
        }
        expectedFormat.encode(image, request.getOutputStream());
    }
}
