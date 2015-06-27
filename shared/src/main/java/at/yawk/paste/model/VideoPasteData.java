package at.yawk.paste.model;

import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class VideoPasteData implements PasteData {
    /**
     * Format is currently always WebM.
     */
    private byte[] video;
}
