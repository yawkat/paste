package at.yawk.paste.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yawkat
 */
class ConfigProvider extends AbstractModule {
    @Provides
    ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    @Provides
    Config config() {
        Path configPath = Paths.get("config.yml");

        if (!Files.exists(configPath)) {
            return new Config(); // default config
        } else {
            // load from file
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.findAndRegisterModules();
            try (InputStream in = Files.newInputStream(configPath)) {
                return objectMapper.readValue(in, Config.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    protected void configure() {

    }
}
