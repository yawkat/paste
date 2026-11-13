package at.yawk.paste.model;

import java.time.Instant;

/**
 * @author yawkat
 */
public record Paste(String id, Instant time, PasteData data) {}
