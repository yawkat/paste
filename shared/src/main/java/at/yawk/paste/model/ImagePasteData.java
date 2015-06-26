package at.yawk.paste.model;

import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class ImagePasteData implements PasteData {
    private byte[] data;
    private ImageFormat format;
}
