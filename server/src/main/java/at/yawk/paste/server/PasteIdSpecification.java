package at.yawk.paste.server;

import java.util.random.RandomGenerator;

/**
 * @author yawkat
 */
public interface PasteIdSpecification {
    /**
     * Regular expression that matches all valid IDs.
     */
    String getPattern();

    String generate(RandomGenerator random);
}
