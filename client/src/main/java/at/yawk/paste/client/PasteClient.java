package at.yawk.paste.client;

import at.yawk.paste.model.PasteData;
import tools.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.*;
import java.util.Base64;
import lombok.SneakyThrows;
import tools.jackson.dataformat.cbor.CBORMapper;

/**
 * @author yawkat
 */
public class PasteClient {
    private final Config config;
    private final ObjectMapper jsonObjectMapper;
    private final ObjectMapper cborObjectMapper;
    // redirects are not followed by default
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private KeyPair keyPair;

    public PasteClient(Config config, ObjectMapper jsonObjectMapper) {
        this.config = config;
        this.jsonObjectMapper = jsonObjectMapper;
        this.cborObjectMapper = CBORMapper.builder().build();
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    private synchronized KeyPair getKeyPair() throws IOException {
        if (keyPair == null) {
            if (Files.exists(config.getKeyFile())) {
                try (InputStream in = Files.newInputStream(config.getKeyFile())) {
                    keyPair = jsonObjectMapper.readValue(in, RsaKeyPair.class).toKeyPair();
                } catch (GeneralSecurityException e) {
                    throw new IOException(e);
                }
            } else {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(4096);
                keyPair = generator.generateKeyPair();
                try (OutputStream out = Files.newOutputStream(config.getKeyFile())) {
                    jsonObjectMapper.writeValue(out, RsaKeyPair.ofKeyPair(keyPair));
                }
            }
        }
        return keyPair;
    }

    public String save(PasteData data) throws IOException {
        return save(data, UploadProgressListener.NOOP);
    }

    /**
     * Save the given paste data.
     *
     * @return The full URL where this paste was saved.
     */
    public String save(PasteData data, UploadProgressListener progressListener) throws IOException {
        byte[] bytes = cborObjectMapper.writeValueAsBytes(data);
        progressListener.update(0, bytes.length);

        KeyPair keyPair = getKeyPair();

        // Authorization: Signature <public key base64> <signature base64>
        String authorization;
        try {
            Signature signature = Signature.getInstance("SHA512withRSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(bytes);
            authorization = "Signature "
                    + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()) + " "
                    + Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }

        URI remote = config.getRemote();
        HttpRequest request = HttpRequest.newBuilder(remote)
                .header("Authorization", authorization)
                .header("Content-Type", "application/cbor")
                .POST(HttpRequest.BodyPublishers.fromPublisher(
                        HttpRequest.BodyPublishers.ofInputStream(() -> new ProgressInputStream(bytes, progressListener)),
                        bytes.length))
                .build();

        HttpResponse<Void> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException();
        }
        if (response.statusCode() >= 300) {
            throw new IOException("Status: " + response.statusCode());
        }

        String targetLocation = response.headers().firstValue("Location")
                .orElseThrow(() -> new IOException("Missing Location header"));
        return remote.getPath().endsWith("/") ? remote + targetLocation : remote + "/" + targetLocation;
    }

    private static final class ProgressInputStream extends FilterInputStream {
        private final long total;
        private final UploadProgressListener listener;
        private long done = 0;

        ProgressInputStream(byte[] bytes, UploadProgressListener listener) {
            super(new ByteArrayInputStream(bytes));
            this.total = bytes.length;
            this.listener = listener;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                listener.update(++done, total);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n > 0) {
                done += n;
                listener.update(done, total);
            }
            return n;
        }
    }
}
