package at.yawk.paste.server;

import at.yawk.paste.server.db.CachedSqlDatabase;
import at.yawk.paste.server.db.Database;
import at.yawk.paste.server.servlet.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * @author yawkat
 */
public class Main {
    public static void main(String[] args) throws IOException {
        Config config = loadConfig(Path.of("config.yml"));
        ObjectMapper jsonMapper = JsonMapper.builder().build();
        PasteIdSpecification pasteIdSpecification = new DefaultPasteIdSpecification();
        Database database = new CachedSqlDatabase(config, pasteIdSpecification);
        PasteServlet.Context context =
                new PasteServlet.Context(database, new UmamiClient(config, jsonMapper), pasteIdSpecification);

        Server server = new Server(config, new TemplateEngine(), database, List.of(
                new ImageDisplayServlet(context),
                new NotFoundServlet(),
                new RawImageServlet(context),
                new RawSVGServlet(context),
                new RawTextServlet(context),
                new RawVideoServlet(context),
                new SVGDisplayServlet(context),
                new TextDisplayServlet(context),
                new TwitterCardImageServlet(context),
                new UploadServlet(config),
                new URLForwardServlet(context),
                new VideoDisplayServlet(context)
        ));
        server.start();
    }

    private static Config loadConfig(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new Config(); // default config
        }
        try (var in = Files.newInputStream(path)) {
            return YAMLMapper.builder().build().readValue(in, Config.class);
        }
    }
}
