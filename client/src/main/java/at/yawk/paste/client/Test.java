package at.yawk.paste.client;

import at.yawk.paste.model.PasteData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * @author yawkat
 */
public class Test {
    public static void main(String[] args) throws IOException {
        Config config = new Config();
        ClipboardHelper clipboardHelper = new ClipboardHelper(config);
        PasteClient pasteClient = new PasteClient(config, new ObjectMapper());

        PasteData data = clipboardHelper.getCurrentClipboardData();
        if (data == null) {
            System.out.println("no data");
            return;
        }
        String url = pasteClient.save(data);
        System.out.println(url);
    }
}
