package casa.squawk7777;

import casa.squawk7777.exceptions.InvalidSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class SecurityHelper {
    private static final Logger log = LoggerFactory.getLogger(SecurityHelper.class);
    private static final String DEFAULT_ALGORITHM = "RSA";
    private static final Integer DEFAULT_KEY_SIZE = 1024;

    private SecurityHelper() {}

    public static byte[] signData(String data, PrivateKey privateKey) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] dataSignature = signature.sign();
        if (log.isDebugEnabled()) {
            log.debug("Generated data signature: {}", Base64.getEncoder().encodeToString(dataSignature));
        }
        return dataSignature;
    }

    public static void verifySignature(String data, byte[] dataSignature, PublicKey publicKey) throws GeneralSecurityException, InvalidSignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        if (!signature.verify(dataSignature)) {
            throw new InvalidSignatureException("Signature for this data is invalid!");
        }
        log.debug("Data signature successfully verified.");
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);
            keyPairGenerator.initialize(DEFAULT_KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            log.debug("Generated KeyPair with Public key: {}", keyPair.getPublic());
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
