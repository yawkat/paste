package at.yawk.paste.model;

import java.net.URL;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class URLPasteData implements PasteData {
    private URL url;
}
