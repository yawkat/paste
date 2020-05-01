package at.yawk.paste.server.db;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.Deserializers;
import java.time.Instant;

class FloatInstantModule extends Module {
    @Override
    public String getModuleName() {
        return "";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addDeserializers(new Deserializers.Base() {
            @Override
            public JsonDeserializer<?> findBeanDeserializer(
                    JavaType type,
                    DeserializationConfig config,
                    BeanDescription beanDesc
            ) throws JsonMappingException {
                if (type.hasRawClass(Instant.class)) {
                    return new FloatInstantDeserializer();
                }
                return super.findBeanDeserializer(type, config, beanDesc);
            }
        });
    }
}
