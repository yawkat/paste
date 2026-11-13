package at.yawk.paste.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author yawkat
 */
record RsaKeyPair(@JsonProperty("private") String privateKey, @JsonProperty("public") String publicKey) {
    public static RsaKeyPair ofKeyPair(KeyPair keyPair) {
        return new RsaKeyPair(
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
        );
    }

    public KeyPair toKeyPair() throws GeneralSecurityException {
        byte[] priEnc = Base64.getDecoder().decode(privateKey);
        byte[] pubEnc = Base64.getDecoder().decode(publicKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return new KeyPair(
                keyFactory.generatePublic(new X509EncodedKeySpec(pubEnc)),
                keyFactory.generatePrivate(new PKCS8EncodedKeySpec(priEnc))
        );
    }
}
