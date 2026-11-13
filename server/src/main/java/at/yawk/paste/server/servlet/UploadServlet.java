package at.yawk.paste.server.servlet;

import at.yawk.paste.model.Paste;
import at.yawk.paste.model.PasteData;
import at.yawk.paste.server.Config;
import at.yawk.paste.server.MsgpackReader;
import at.yawk.paste.server.Request;
import at.yawk.paste.server.Servlet;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.cbor.CBORMapper;

/**
 * @author yawkat
 */
public class UploadServlet implements Servlet {
    private final Config config;
    private volatile PublicKey publicKey;

    private final ObjectMapper cborMapper = CBORMapper.builder().build();

    public UploadServlet(Config config) {
        this.config = config;
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

        String[] parts = authorization.getFirst().split(" ");
        if (parts.length != 3 || !parts[0].equals("Signature")) {
            noAccess(request);
            return;
        }

        PublicKey publicKey = decodePublicKey(Base64.getDecoder().decode(parts[1]));
        if (this.publicKey == null) {
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
        byte[] bytes = request.getInputStream().readAllBytes();

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

        PasteData pasteData;
        HeaderValues contentType = request.getExchange().getRequestHeaders().get("Content-Type");
        if (contentType != null && contentType.stream().anyMatch(v -> v.contains("application/cbor"))) {
            pasteData = cborMapper.readValue(bytes, PasteData.class);
        } else {
            // clients from before the switch to CBOR
            pasteData = cborMapper.treeToValue(MsgpackReader.read(bytes), PasteData.class);
        }
        Paste inserted = request.getDatabase().insertPaste(pasteData);

        request.getExchange().getResponseHeaders().add(new HttpString("Location"), inserted.id());
        request.getExchange().setStatusCode(201);
        request.finish();
    }

    protected PublicKey decodePublicKey(byte[] bytes) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    private static void noAccess(Request request) {
        request.getExchange().setStatusCode(403);
        request.finish();
    }
}
