package at.yawk.paste.server;

import java.util.random.RandomGenerator;

/**
 * @author yawkat
 */
class DefaultPasteIdSpecification implements PasteIdSpecification {
    private static final char[] VALID_CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int LENGTH = 8;

    @Override
    public String getPattern() {
        return "[a-zA-Z0-9]+";
    }

    @Override
    public String generate(RandomGenerator random) {
        char[] chars = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            chars[i] = VALID_CHARACTERS[random.nextInt(VALID_CHARACTERS.length)];
        }
        return new String(chars);
    }
}
