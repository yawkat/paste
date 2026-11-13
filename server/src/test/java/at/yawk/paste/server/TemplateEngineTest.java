package at.yawk.paste.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import at.yawk.paste.model.ImageFormat;
import at.yawk.paste.model.ImagePasteData;
import at.yawk.paste.model.Paste;
import at.yawk.paste.server.servlet.TextDisplayServlet;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TemplateEngineTest {
    private final TemplateEngine templateEngine = new TemplateEngine();

    private String render(String view, Object model) throws IOException {
        StringWriter writer = new StringWriter();
        templateEngine.render(view, writer, model);
        return writer.toString();
    }

    @Test
    void image() throws IOException {
        String html = render("image", new Paste("abc", Instant.now(), new ImagePasteData(new byte[0], ImageFormat.JPEG)));
        assertTrue(html.contains("<img src=\"abc.jpg\">"), html);
    }

    @Test
    void text() throws IOException {
        String html = render("text", new TextDisplayServlet.View(
                "abc", new TextDisplayServlet.SplittedText(null, new String[]{ "a < b", "c" })));
        assertTrue(html.contains("<span>a &lt; b<br></span><span>c<br></span>"), html);
        assertTrue(html.contains("highlight.min.js"), html);
    }

    @Test
    void notFound() throws IOException {
        assertTrue(render("404", null).contains("Not Found"));
    }
}
