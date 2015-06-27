package at.yawk.paste.client;

/**
 * @author yawkat
 */
public interface UploadProgressListener {
    UploadProgressListener NOOP = (done, total) -> {};

    void update(long done, long total);
}
