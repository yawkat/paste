package at.yawk.paste.model;

/**
 * @param video Format is currently always WebM.
 * @author yawkat
 */
public record VideoPasteData(byte[] video) implements PasteData {}
