package at.yawk.paste.server.servlet;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.server.Config;
import at.yawk.paste.server.Request;
import at.yawk.paste.server.Servlet;
import at.yawk.yarn.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 * @author yawkat
 */
@Component
public class UploadServlet implements Servlet {
    private PublicKey publicKey;
    @Inject Config config;

    private final ObjectMapper msgPackMapper;

    {
        msgPackMapper = new ObjectMapper(new MessagePackFactory());
        msgPackMapper.findAndRegisterModules();
    }

    @PostConstruct
    void loadKey() {
        if (Files.exists(config.getKeyLocation())) {
            try {
                publicKey = decodePublicKey(Files.readAllBytes(config.getKeyLocation()));
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void handle(Request request) throws Exception {
        // only accept POST /
        if (!request.getExchange().getRequestMethod().equalToString("POST") ||
            !request.getExchange().getRelativePath().equals("/")) {
            request.proceed();
            return;
        }

        HeaderValues authorization = request.getExchange().getRequestHeaders().get("Authorization");
        if (authorization == null || authorization.size() != 1) {
            noAccess(request);
            return;
        }

        String[] parts = authorization.get(0).split(" ");
        if (parts.length != 3 || !parts[0].equals("Signature")) {
            noAccess(request);
            return;
        }

        PublicKey publicKey = decodePublicKey(Base64.getDecoder().decode(parts[1]));
        if (this.publicKey == null) {
            // in theory we'd have to start blocking here but I'm lazy and it's only called once or twice
            synchronized (this) {
                if (this.publicKey == null) {
                    Files.write(config.getKeyLocation(), publicKey.getEncoded());
                    this.publicKey = publicKey;
                }
            }
        }
        if (!publicKey.equals(this.publicKey)) {
            noAccess(request);
            return;
        }

        byte[] signature = Base64.getDecoder().decode(parts[2]);

        if (request.getExchange().isInIoThread()) {
            request.getExchange().dispatch(exchange -> handleBlocking(request, signature));
        } else {
            handleBlocking(request, signature);
        }
    }

    private void handleBlocking(Request request, byte[] signature) throws IOException {
        byte[] bytes = ByteStreams.toByteArray(request.getInputStream());

        try {
            Signature verifier = Signature.getInstance("SHA512withRSA");
            verifier.initVerify(publicKey);
            verifier.update(bytes);
            if (!verifier.verify(signature)) {
                noAccess(request);
                return;
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new IOException(e);
        }

        PasteData pasteData = msgPackMapper.readValue(bytes, PasteData.class);
        Paste inserted = request.getDatabase().insertPaste(pasteData);

        request.getExchange().getResponseHeaders().add(new HttpString("Location"), inserted.getId());
        request.getExchange().setResponseCode(201);
        request.finish();
    }

    protected PublicKey decodePublicKey(byte[] bytes) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    private static void noAccess(Request request) {
        request.getExchange().setResponseCode(403);
        request.finish();
    }
}
