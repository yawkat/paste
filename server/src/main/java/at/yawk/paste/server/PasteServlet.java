package at.yawk.paste.server;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.server.db.Database;
import java.lang.annotation.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * @author yawkat
 */
public abstract class PasteServlet<D extends PasteData> implements Servlet {
    private final Class<D> dataType;
    private final Pattern pattern;
    private final Database database;
    private final UmamiClient umamiClient;

    @SuppressWarnings("unchecked")
    protected PasteServlet(Context context) {
        this.database = context.database();
        this.umamiClient = context.umamiClient();

        Class<D> dataType = null;
        Class<?> c = getClass();
        while (c != PasteServlet.class) {
            Type gen = c.getGenericSuperclass();
            if (gen instanceof ParameterizedType parameterized
                    && parameterized.getActualTypeArguments()[0] instanceof Class<?> arg) {
                dataType = (Class<D>) arg;
            }
            c = c.getSuperclass();
        }
        if (dataType == null) {
            throw new NullPointerException("Could not find data type for " + getClass().getName());
        }
        this.dataType = dataType;
        // getSuffixPattern implementations must not depend on subclass state
        this.pattern = Pattern.compile("/(" + context.pasteIdSpecification().getPattern() + ")" + getSuffixPattern(),
                                       Pattern.CASE_INSENSITIVE);
    }

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
        Optional<Paste> paste = database.getPaste(id);
        if (paste.isPresent() && dataType.isInstance(paste.get().data())) {
            List<String> additionalGroups = IntStream.rangeClosed(2, matcher.groupCount())
                    .mapToObj(matcher::group)
                    .toList();

            umamiClient.trackView(request.getExchange(), id);
            handle(request, paste.get(), dataType.cast(paste.get().data()), additionalGroups);
            request.finish();
        } else {
            // try the other handlers
            request.proceed();
        }
    }

    protected abstract void handle(Request request, Paste paste, D data, List<String> groups) throws Exception;

    /**
     * What every paste servlet needs from the server.
     */
    public record Context(Database database, UmamiClient umamiClient, PasteIdSpecification pasteIdSpecification) {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface Suffix {
        String value();
    }
}
