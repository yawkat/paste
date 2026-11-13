package at.yawk.paste.server;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reads MessagePack into a Jackson tree, so it can be bound like any other format. Only used for pastes that were
 * stored or uploaded as MessagePack before the switch to CBOR. Extension types are not supported, since the old
 * Jackson MessagePack backend never wrote them for paste data.
 */
public final class MsgpackReader {
    private static final int MAX_DEPTH = 64;

    private final ByteBuffer buffer;
    private final JsonNodeFactory nodes = JsonNodeFactory.instance;

    private MsgpackReader(byte[] bytes) {
        this.buffer = ByteBuffer.wrap(bytes);
    }

    public static JsonNode read(byte[] bytes) throws IOException {
        MsgpackReader reader = new MsgpackReader(bytes);
        try {
            JsonNode node = reader.readValue(0);
            if (reader.buffer.hasRemaining()) {
                throw new IOException("Trailing data after MessagePack value");
            }
            return node;
        } catch (BufferUnderflowException e) {
            throw new IOException("Truncated MessagePack value", e);
        }
    }

    private JsonNode readValue(int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("MessagePack value nested too deeply");
        }
        int type = buffer.get() & 0xff;
        return switch (type) {
            case 0xc0 -> nodes.nullNode();
            case 0xc2 -> nodes.booleanNode(false);
            case 0xc3 -> nodes.booleanNode(true);
            case 0xc4 -> nodes.binaryNode(readBytes(buffer.get() & 0xff));
            case 0xc5 -> nodes.binaryNode(readBytes(buffer.getShort() & 0xffff));
            case 0xc6 -> nodes.binaryNode(readBytes(buffer.getInt() & 0xffffffffL));
            case 0xca -> nodes.numberNode(buffer.getFloat());
            case 0xcb -> nodes.numberNode(buffer.getDouble());
            case 0xcc -> nodes.numberNode(buffer.get() & 0xff);
            case 0xcd -> nodes.numberNode(buffer.getShort() & 0xffff);
            case 0xce -> nodes.numberNode(buffer.getInt() & 0xffffffffL);
            case 0xcf -> {
                long value = buffer.getLong();
                yield value >= 0
                        ? nodes.numberNode(value)
                        : nodes.numberNode(new BigInteger(Long.toUnsignedString(value)));
            }
            case 0xd0 -> nodes.numberNode(buffer.get());
            case 0xd1 -> nodes.numberNode(buffer.getShort());
            case 0xd2 -> nodes.numberNode(buffer.getInt());
            case 0xd3 -> nodes.numberNode(buffer.getLong());
            case 0xd9 -> nodes.stringNode(readString(buffer.get() & 0xff));
            case 0xda -> nodes.stringNode(readString(buffer.getShort() & 0xffff));
            case 0xdb -> nodes.stringNode(readString(buffer.getInt() & 0xffffffffL));
            case 0xdc -> readArray(buffer.getShort() & 0xffff, depth);
            case 0xdd -> readArray(buffer.getInt() & 0xffffffffL, depth);
            case 0xde -> readMap(buffer.getShort() & 0xffff, depth);
            case 0xdf -> readMap(buffer.getInt() & 0xffffffffL, depth);
            default -> {
                if (type <= 0x7f) {
                    yield nodes.numberNode(type);
                } else if (type <= 0x8f) {
                    yield readMap(type & 0x0f, depth);
                } else if (type <= 0x9f) {
                    yield readArray(type & 0x0f, depth);
                } else if (type <= 0xbf) {
                    yield nodes.stringNode(readString(type & 0x1f));
                } else if (type >= 0xe0) {
                    yield nodes.numberNode((byte) type);
                } else {
                    throw new IOException("Unsupported MessagePack type 0x" + Integer.toHexString(type));
                }
            }
        };
    }

    private byte[] readBytes(long length) throws IOException {
        if (length > buffer.remaining()) {
            throw new IOException("Truncated MessagePack value");
        }
        byte[] bytes = new byte[(int) length];
        buffer.get(bytes);
        return bytes;
    }

    private String readString(long length) throws IOException {
        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    private ArrayNode readArray(long size, int depth) throws IOException {
        ArrayNode array = nodes.arrayNode();
        for (long i = 0; i < size; i++) {
            array.add(readValue(depth + 1));
        }
        return array;
    }

    private ObjectNode readMap(long size, int depth) throws IOException {
        ObjectNode object = nodes.objectNode();
        for (long i = 0; i < size; i++) {
            JsonNode key = readValue(depth + 1);
            if (!key.isString()) {
                throw new IOException("Unsupported non-string MessagePack map key");
            }
            object.set(key.asString(), readValue(depth + 1));
        }
        return object;
    }
}
