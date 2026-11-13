package at.yawk.paste.server.servlet;

import at.yawk.paste.model.ImageFormat;
import at.yawk.paste.model.ImagePasteData;
import at.yawk.paste.model.Paste;
import at.yawk.paste.server.PasteServlet;
import at.yawk.paste.server.Request;
import io.undertow.util.HttpString;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author yawkat
 */
public class RawImageServlet extends PasteServlet<ImagePasteData> {
    public RawImageServlet(Context context) {
        super(context);
    }

    @Override
    protected String getSuffixPattern() {
        // for example \.(png|jpeg|jpg)
        return // leading dot
                "\\." +

                // get extensions
                Arrays.stream(ImageFormat.values())
                        .flatMap(format -> format.getExtensions().stream())
                                // escape extension
                        .map(Pattern::quote)
                                // join into capture group
                        .collect(Collectors.joining("|", "(", ")"));
    }

    @Override
    protected void handle(Request request, Paste paste, ImagePasteData data, List<String> groups) throws Exception {
        ImageFormat expectedFormat = ImageFormat.byExtension(groups.getFirst().toLowerCase(Locale.ROOT));

        if (expectedFormat == null) {
            // should probably not happen but handle it anyway
            request.proceed();
            return;
        }

        request.getExchange().setStatusCode(200);
        request.getExchange().getResponseHeaders().add(new HttpString("Content-Type"), expectedFormat.getMediaType());

        dumpImage(request, data, expectedFormat);

        request.finish();
    }

    protected void dumpImage(Request request, ImagePasteData data, ImageFormat expectedFormat) throws IOException {
        byte[] bytes = data.data();
        if (data.format() == expectedFormat) {
            request.setContentLength(bytes.length);
            request.getOutputStream().write(bytes);
        } else {
            data.format().transformTo(new ByteArrayInputStream(bytes), expectedFormat, request.getOutputStream());
        }
    }
}
