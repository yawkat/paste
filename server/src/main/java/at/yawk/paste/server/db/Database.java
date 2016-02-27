package at.yawk.paste.server.db;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import com.google.inject.ImplementedBy;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * @author yawkat
 */
@ImplementedBy(CachedMongoDatabase.class)
public interface Database {
    /**
     * Get the paste with the given ID. May block only if the block parameter is true.
     *
     * @return The saved paste with this id or an empty optional if this paste does not exist or null if block is false
     * and we don't know if this paste exists. The returned paste must never be modified.
     */
    @Nullable
    Optional<Paste> getPaste(String id, boolean block);

    /**
     * Create a paste with the given data. May block.
     */
    Paste insertPaste(PasteData data);
}
