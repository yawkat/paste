package at.yawk.paste.client;

import at.yawk.paste.model.PasteData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.*;
import java.util.Base64;
import lombok.SneakyThrows;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 * @author yawkat
 */
public class PasteClient {
    private final Config config;
    private final ObjectMapper jsonObjectMapper;
    private final ObjectMapper msgpackObjectMapper;

    private KeyPair keyPair;

    public PasteClient(Config config, ObjectMapper jsonObjectMapper) {
        this.config = config;
        this.jsonObjectMapper = jsonObjectMapper;
        this.msgpackObjectMapper = new ObjectMapper(new MessagePackFactory());
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    private KeyPair getKeyPair() throws IOException {
        if (keyPair == null) {
            synchronized (this) {
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
        }
        return keyPair;
    }

    /**
     * Save the given paste data.
     *
     * @return The full URL where this paste was saved.
     */
    public String save(PasteData data) throws IOException {
        byte[] bytes = msgpackObjectMapper.writeValueAsBytes(data);

        URL url = config.getRemote();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        KeyPair keyPair = getKeyPair();

        // build authorization header

        // Authorization: Signature <public key base64> <signature base64>
        StringBuilder authHeaderBuilder = new StringBuilder("Signature ");
        // public key
        authHeaderBuilder.append(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        authHeaderBuilder.append(' ');
        // signature
        try {
            Signature signature = Signature.getInstance("SHA512withRSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(bytes);
            authHeaderBuilder.append(Base64.getEncoder().encodeToString(signature.sign()));
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }

        connection.setRequestProperty("Authorization", authHeaderBuilder.toString());

        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);

        connection.setFixedLengthStreamingMode(bytes.length);
        connection.getOutputStream().write(bytes);
        connection.getOutputStream().close();

        String targetLocation = connection.getHeaderField("Location");

        connection.disconnect();

        URL remote = config.getRemote();
        return remote.getPath().endsWith("/") ? remote + targetLocation : remote + "/" + targetLocation;
    }
}