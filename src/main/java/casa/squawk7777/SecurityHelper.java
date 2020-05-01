package casa.squawk7777;

import casa.squawk7777.exceptions.InvalidSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class SecurityHelper {
    private static final Logger log = LoggerFactory.getLogger(SecurityHelper.class);
    private static final String defaultAlgorithm = "RSA";
    private static final Integer defaultKeySize = 1024;

    private SecurityHelper() {}

    public static byte[] signData(String data, PrivateKey privateKey) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        return signature.sign();
    }

    public static void verifySignature(String data, byte[] dataSignature, PublicKey publicKey) throws GeneralSecurityException, InvalidSignatureException {
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes());
        if (!signature.verify(dataSignature)) {
            throw new InvalidSignatureException("Signature for this data is invalid!");
        }
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(defaultAlgorithm);
            keyPairGenerator.initialize(defaultKeySize);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
