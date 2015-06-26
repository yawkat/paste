package at.yawk.paste.server;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.server.db.Database;
import io.undertow.server.HttpServerExchange;
import java.lang.annotation.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.RegEx;
import javax.inject.Inject;

/**
 * @author yawkat
 */
public abstract class PasteServlet<D extends PasteData> implements Servlet {
    private final Class<D> dataType;
    private Pattern pattern;

    @Inject Database database;

    @SuppressWarnings("unchecked")
    public PasteServlet() {
        Class<D> dataType = null;
        Class<?> c = getClass();
        while (c != PasteServlet.class) {
            Type gen = c.getGenericSuperclass();
            if (gen instanceof ParameterizedType) {
                dataType = (Class<D>) ((ParameterizedType) gen).getActualTypeArguments()[0];
            }
            c = c.getSuperclass();
        }
        if (dataType == null) {
            throw new NullPointerException("Could not find data type for " + getClass().getName());
        }
        this.dataType = dataType;
    }

    @Inject
    public void buildPattern(PasteIdSpecification specification) {
        Suffix suffixAnnotation = getClass().getAnnotation(Suffix.class);
        if (suffixAnnotation == null) {
            throw new NullPointerException(getClass().getName() + " is missing @Suffix annotation");
        }
        pattern = Pattern.compile("(" + specification.getPattern() + ")" + suffixAnnotation.value());
    }

    @Override
    public void handle(HttpServerExchange request) {
        Matcher matcher = pattern.matcher(request.getRelativePath());
        if (matcher.matches()) {
            handle0(request, matcher);
        }
    }

    private void handle0(HttpServerExchange request, Matcher matcher) {
        String id = matcher.group(1);
        Optional<Paste> paste = database.getPaste(id, false);
        if (paste == null) {
            if (request.isInIoThread()) {
                // move to worker thread
                request.startBlocking();
                request.dispatch(exchange -> {
                    handle0(request, matcher);
                });
                return;
            } else {
                paste = database.getPaste(id, true);
                assert paste != null;
            }
        }

        if (paste.isPresent()) {
            if (dataType.isInstance(paste.get().getData())) {
                handle(request, paste.get());
                request.endExchange();
                return;
            }
        }

        request.setResponseCode(404);
        request.endExchange();
    }

    protected abstract void handle(HttpServerExchange exchange, Paste paste);

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    protected @interface Suffix {
        @RegEx String value();
    }
}
