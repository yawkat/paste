package at.yawk.paste.server;

import com.google.inject.ImplementedBy;
import java.util.Random;
import javax.annotation.RegEx;

/**
 * @author yawkat
 */
@ImplementedBy(DefaultPasteIdSpecification.class)
public interface PasteIdSpecification {
    /**
     * Regular expression that matches all valid IDs.
     */
    @RegEx
    String getPattern();

    String generate(Random random);
}
