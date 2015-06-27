package at.yawk.paste.model;

import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class SVGPasteData implements PasteData {
    private String svg;
}
