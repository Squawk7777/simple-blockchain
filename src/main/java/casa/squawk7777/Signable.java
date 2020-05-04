package casa.squawk7777;

import java.security.PublicKey;

public interface Signable {
    String getDigest();

    byte[] getSignature();

    PublicKey getPublicKey();

    void sign(byte[] signature, PublicKey publicKey);
}
