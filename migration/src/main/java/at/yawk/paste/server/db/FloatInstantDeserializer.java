package at.yawk.paste.server.db;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

class FloatInstantDeserializer extends InstantDeserializer<Instant> {
    public FloatInstantDeserializer() {
        super(Instant.class,
              DateTimeFormatter.ISO_INSTANT,
              Instant::from,
              a -> Instant.ofEpochMilli(a.value),
              a -> Instant.ofEpochSecond(a.integer, a.fraction),
              null,
              true
        );
    }

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.getCurrentTokenId() == JsonTokenId.ID_NUMBER_FLOAT &&
            (parser.isNaN() || Double.isNaN(parser.getDoubleValue()))) {
            return null;
        }

        return super.deserialize(parser, context);
    }
}
