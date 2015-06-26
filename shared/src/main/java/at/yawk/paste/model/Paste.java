package at.yawk.paste.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

/**
 * @author yawkat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Wither
public class Paste {
    // serialize to _id for mongo
    @JsonProperty("_id") private String id;
    private PasteData data;
}
