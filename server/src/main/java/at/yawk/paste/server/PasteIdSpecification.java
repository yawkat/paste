package at.yawk.paste.server;

import java.util.Random;
import javax.annotation.RegEx;

/**
 * @author yawkat
 */
public interface PasteIdSpecification {
    /**
     * Regular expression that matches all valid IDs.
     */
    @RegEx
    String getPattern();

    String generate(Random random);
}
