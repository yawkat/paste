package at.yawk.paste.server;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.Writer;
import javax.inject.Singleton;
import lombok.SneakyThrows;

/**
 * @author yawkat
 */
@Singleton
public class TemplateEngine {
    private Configuration cfg;

    {
        cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setClassLoaderForTemplateLoading(TemplateEngine.class.getClassLoader(), "/templates/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    private Template getTemplate(String name) throws IOException {
        return cfg.getTemplate(name + ".ftl");
    }

    @SneakyThrows(TemplateException.class)
    public void render(String viewName, Writer writer, Object model) throws IOException {
        Template template = getTemplate(viewName);
        template.process(model, writer);
    }
}
