package at.yawk.paste.server;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.server.db.Database;
import java.lang.annotation.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
                Type arg = ((ParameterizedType) gen).getActualTypeArguments()[0];
                if (arg instanceof Class) {
                    dataType = (Class<D>) arg;
                }
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
        pattern = Pattern.compile("/(" + specification.getPattern() + ")" + getSuffixPattern(),
                                  Pattern.CASE_INSENSITIVE);
    }

    @RegEx
    protected String getSuffixPattern() {
        Suffix suffixAnnotation = getClass().getAnnotation(Suffix.class);
        if (suffixAnnotation == null) {
            throw new NullPointerException(getClass().getName() + " is missing @Suffix annotation");
        }
        return suffixAnnotation.value();
    }

    @Override
    public void handle(Request request) throws Exception {
        if (request.getExchange().getRequestMethod().equalToString("GET")) {
            Matcher matcher = pattern.matcher(request.getExchange().getRelativePath());
            if (matcher.matches()) {
                handle0(request, matcher);
                return;
            }
        }
        request.proceed();
    }

    private void handle0(Request request, Matcher matcher) throws Exception {
        String id = matcher.group(1);
        Optional<Paste> paste = database.getPaste(id, false);
        if (paste == null) {
            if (request.getExchange().isInIoThread()) {
                // move to worker thread
                request.getExchange().dispatch(exchange -> {
                    handle0(request, matcher);
                });
                return;
            } else {
                paste = database.getPaste(id, true);
                assert paste != null;
            }
        }

        if (paste.isPresent() && dataType.isInstance(paste.get().getData())) {
            List<String> additionalGroups;
            if (matcher.groupCount() <= 1) {
                additionalGroups = Collections.emptyList();
            } else {
                additionalGroups = new ArrayList<>(matcher.groupCount() - 1);
                for (int i = 2; i <= matcher.groupCount(); i++) {
                    additionalGroups.add(matcher.group(i));
                }
            }

            //noinspection unchecked
            handle(request, paste.get(), (D) paste.get().getData(), additionalGroups);
            request.finish();
        } else {
            // try the other handlers
            request.proceed();
        }
    }

    protected abstract void handle(Request request, Paste paste, D data, List<String> groups) throws Exception;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface Suffix {
        @RegEx String value();
    }
}
