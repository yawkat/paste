package at.yawk.paste.model;

/**
 * @author yawkat
 */
public record ImagePasteData(byte[] data, ImageFormat format) implements PasteData {}
