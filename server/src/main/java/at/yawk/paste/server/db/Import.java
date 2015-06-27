package at.yawk.paste.server.db;

import at.yawk.paste.model.*;
import at.yawk.paste.server.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;

/**
 * @author yawkat
 */
class Import {
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        CachedMongoDatabase db = new CachedMongoDatabase();
        db.connect(new Config(), objectMapper);

        Iterator<String> iterator = Files.lines(Paths.get("/home/yawkat/.config/herbstluftwm/screenshot/db.json"))
                .iterator();

        outer:
        while (iterator.hasNext()) {
            String line = iterator.next();
            JsonNode node = objectMapper.readTree(line);
            JsonNode value = node.get("value");

            if (value == null || value.get("type") == null) { continue; }

            PasteData data;

            String type = value.get("type").asText();
            switch (type) {
            case "code":
                data = new TextPasteData();
                ((TextPasteData) data).setText(new String(decodeExportedString(value.get("code_text")), CHARSET));
                break;
            case "image":
                String imageFormat = value.get("image_format").asText();
                switch (imageFormat) {
                case "png":
                    data = new ImagePasteData();
                    ((ImagePasteData) data).setFormat(ImageFormat.PNG);
                    ((ImagePasteData) data).setData(decodeExportedString(value.get("image_blob")));
                    break;
                case "svg":
                    data = new SVGPasteData();
                    ((SVGPasteData) data).setSvg(new String(decodeExportedString(value.get("image_blob")), CHARSET));
                    break;
                default:
                    System.err.println("Unsupported image format " + imageFormat);
                    continue outer;
                }
                break;
            case "svg":
                data = new SVGPasteData();
                ((SVGPasteData) data).setSvg(new String(decodeExportedString(value.get("svg_blob")), CHARSET));
                break;
            case "url":
                data = new URLPasteData();
                ((URLPasteData) data).setUrl(new URL(new String(decodeExportedString(value.get("url")), CHARSET)));
                break;
            case "video":
                data = new VideoPasteData();
                ((VideoPasteData) data).setVideo(decodeExportedString(value.get("video_blob")));
                break;
            default:
                System.err.println("Unsupported data type " + type);
                continue outer;
            }

            Paste paste = new Paste();
            paste.setId(node.get("key").asText());
            double time = value.get("time").asDouble();
            paste.setTime(Instant.ofEpochSecond((long) time, (long) (time * 1_000_000_000L)));
            paste.setData(data);
            db.upsert(paste);
        }
    }

    private static byte[] decodeExportedString(JsonNode node) {
        if (node.isTextual()) {
            return node.asText().getBytes(CHARSET);
        } else if (node.isArray()) {
            return Base64.getDecoder().decode(node.get(0).asText());
        } else {
            throw new UnsupportedOperationException("Unexpected node type " + node.getNodeType());
        }
    }
}
