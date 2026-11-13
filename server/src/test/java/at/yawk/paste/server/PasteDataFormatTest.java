package at.yawk.paste.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import at.yawk.paste.model.ImageFormat;
import at.yawk.paste.model.ImagePasteData;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.model.SVGPasteData;
import at.yawk.paste.model.TextPasteData;
import at.yawk.paste.model.URLPasteData;
import at.yawk.paste.model.VideoPasteData;
import java.net.URI;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.komamitsu.jackson.dataformat.msgpack.MessagePackMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.cbor.CBORMapper;

class PasteDataFormatTest {
    private final ObjectMapper cborMapper = CBORMapper.builder().build();
    private final ObjectMapper msgpackMapper = MessagePackMapper.builder().build();

    /**
     * Written by the old Jackson 2 MessagePack backend, which is how data version 1 rows and old uploads look.
     */
    private PasteData readLegacy(String hex) {
        return msgpackMapper.readValue(HexFormat.of().parseHex(hex), PasteData.class);
    }

    @Test
    void legacyText() {
        assertEquals(new TextPasteData("hello wörld\nline 2"),
                     readLegacy("82a474797065a474657874a474657874b368656c6c6f2077c3b6726c640a6c696e652032"));
    }

    @Test
    void legacyImage() {
        ImagePasteData image = assertInstanceOf(ImagePasteData.class, readLegacy(
                "83a474797065a5696d616765a464617461c406ffd8ff000102a6666f726d6174a44a504547"));
        assertEquals(ImageFormat.JPEG, image.format());
        assertArrayEquals(new byte[]{ (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 1, 2 }, image.data());
    }

    @Test
    void legacySvg() {
        assertEquals(new SVGPasteData("<svg/>"), readLegacy("82a474797065a3737667a3737667a63c7376672f3e"));
    }

    @Test
    void legacyVideo() {
        VideoPasteData video = assertInstanceOf(VideoPasteData.class, readLegacy(
                "82a474797065a5766964656fa5766964656fc4041a45dfa3"));
        assertArrayEquals(new byte[]{ 0x1a, 0x45, (byte) 0xdf, (byte) 0xa3 }, video.video());
    }

    @Test
    void legacyUrl() {
        URLPasteData url = assertInstanceOf(URLPasteData.class, readLegacy(
                "82a474797065a375726ca375726cb968747470733a2f2f6578616d706c652e636f6d2f613f623d63"));
        assertEquals("https://example.com/a?b=c", url.url().toString());
    }

    @Test
    void malformedMsgpack() {
        // map with one entry, but the value is missing
        assertThrows(JacksonException.class, () -> readLegacy("81a474797065"));
        // str32 claiming more bytes than there are
        assertThrows(JacksonException.class, () -> readLegacy("81a474797065dbffffffff"));
    }

    @Test
    void cborRoundTrip() throws Exception {
        for (PasteData data : new PasteData[]{
                new TextPasteData("text"),
                new SVGPasteData("<svg/>"),
                new URLPasteData(URI.create("https://example.com/").toURL()),
        }) {
            byte[] bytes = cborMapper.writerFor(PasteData.class).writeValueAsBytes(data);
            assertEquals(data, cborMapper.readValue(bytes, PasteData.class));
        }

        byte[] bytes = cborMapper.writerFor(PasteData.class)
                .writeValueAsBytes(new ImagePasteData(new byte[]{ 1, 2, 3 }, ImageFormat.PNG));
        ImagePasteData image = assertInstanceOf(ImagePasteData.class, cborMapper.readValue(bytes, PasteData.class));
        assertArrayEquals(new byte[]{ 1, 2, 3 }, image.data());
        assertEquals(ImageFormat.PNG, image.format());
    }
}
