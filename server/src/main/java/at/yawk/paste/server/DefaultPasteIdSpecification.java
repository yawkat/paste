package at.yawk.paste.server;

import at.yawk.yarn.Component;
import java.util.Random;

/**
 * @author yawkat
 */
@Component
class DefaultPasteIdSpecification implements PasteIdSpecification {
    private static final char[] VALID_CHARACTERS;
    private static final int LENGTH = 4;

    static {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        VALID_CHARACTERS = (alphabet.toLowerCase() + alphabet.toUpperCase() + "0123456789").toCharArray();
    }

    @Override
    public String getPattern() {
        return "[a-zA-Z0-9]+";
    }

    @Override
    public String generate(Random random) {
        char[] chars = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            chars[i] = VALID_CHARACTERS[random.nextInt(VALID_CHARACTERS.length)];
        }
        return new String(chars);
    }
}
