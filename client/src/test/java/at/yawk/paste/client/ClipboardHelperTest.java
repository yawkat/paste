package at.yawk.paste.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ClipboardHelperTest {
    private final ClipboardHelper helper = new ClipboardHelper(new Config());

    @Test
    void detectsCharset() {
        String text = "Grüße aus Österreich, schöne Äpfel und Größenverhältnisse für Übermütige";
        assertEquals(text, helper.getTextPasteData(text.getBytes(StandardCharsets.UTF_8)).text());
        assertEquals("plain ascii", helper.getTextPasteData("plain ascii".getBytes(StandardCharsets.US_ASCII)).text());
    }

    @Test
    void urls() {
        assertNotNull(helper.getUrlPasteData("https://example.com/a?b=c"));
        assertNotNull(helper.getUrlPasteData("https://example.com/search?q[]=a|b"));
        assertNull(helper.getUrlPasteData("note: something"));
        assertNull(helper.getUrlPasteData("relative/path"));
        assertNull(helper.getUrlPasteData("unknown://protocol"));
    }
}
