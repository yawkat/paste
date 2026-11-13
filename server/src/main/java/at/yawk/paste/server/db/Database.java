package at.yawk.paste.server.db;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import java.util.Optional;

/**
 * Requests are handled on virtual threads, so all methods may block.
 *
 * @author yawkat
 */
public interface Database {
    /**
     * Get the paste with the given ID.
     *
     * @return The saved paste with this id or an empty optional if this paste does not exist. The returned paste must
     * never be modified.
     */
    Optional<Paste> getPaste(String id);

    /**
     * Create a paste with the given data.
     */
    Paste insertPaste(PasteData data);
}
