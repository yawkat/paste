package at.yawk.paste.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author yawkat
 */
@JsonSubTypes({
        @JsonSubTypes.Type(value = ImagePasteData.class, name = "image"),
        @JsonSubTypes.Type(value = TextPasteData.class, name = "text"),
        @JsonSubTypes.Type(value = SVGPasteData.class, name = "svg"),
        @JsonSubTypes.Type(value = VideoPasteData.class, name = "video"),
})
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
public interface PasteData {}
