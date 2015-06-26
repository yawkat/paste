package at.yawk.paste.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class Paste {
    // serialize to _id for mongo
    @JsonProperty("_id") private String id;
    private PasteData data;
}
