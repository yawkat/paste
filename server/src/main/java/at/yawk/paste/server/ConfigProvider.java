package at.yawk.paste.server;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;
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
        return JsonMapper.builder().build();
    }

    @Provides
    Config config() {
        Path configPath = Paths.get("config.yml");

        if (!Files.exists(configPath)) {
            return new Config(); // default config
        } else {
            // load from file
            YAMLMapper yamlMapper = YAMLMapper.builder().build();
            try (InputStream in = Files.newInputStream(configPath)) {
                return yamlMapper.readValue(in, Config.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    protected void configure() {

    }
}
