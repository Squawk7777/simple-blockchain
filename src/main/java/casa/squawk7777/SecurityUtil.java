package casa.squawk7777;

import casa.squawk7777.exceptions.InvalidSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.Base64;

public class SecurityUtil {
    private static final Logger log = LoggerFactory.getLogger(SecurityUtil.class);
    private static final String DEFAULT_ALGORITHM = "RSA";
    private static final Integer DEFAULT_KEY_SIZE = 1024;

    private SecurityUtil() {}

    public static void sign(Signable signable, KeyPair keyPair) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(signable.getDigest().getBytes(StandardCharsets.UTF_8));
        byte[] dataSignature = signature.sign();
        if (log.isDebugEnabled()) {
            log.trace("Generated data signature: {}", Base64.getEncoder().encodeToString(dataSignature));
        }
        signable.sign(dataSignature, keyPair.getPublic());
    }

    public static void verifySignature(Signable signable) throws GeneralSecurityException, InvalidSignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(signable.getPublicKey());
        signature.update(signable.getDigest().getBytes(StandardCharsets.UTF_8));
        if (!signature.verify(signable.getSignature())) {
            throw new InvalidSignatureException(TextConstants.SIGNATURE_IS_INVALID);
        }
        log.debug("Data signature successfully verified.");
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);
            keyPairGenerator.initialize(DEFAULT_KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            log.trace("Generated KeyPair with Public key: {}", keyPair.getPublic());
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(TextConstants.UNABLE_TO_GENERATE_KEYS);
        }
    }
}
