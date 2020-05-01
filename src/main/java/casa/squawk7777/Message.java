package casa.squawk7777;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;

public class Message implements WorkloadItem {
    private final Integer id;
    private final String text;
    private final byte[] signature;
    private final PublicKey publicKey;

    public Message(Integer id, String text, byte[] signature, PublicKey publicKey) {
        this.id = id;
        this.text = text;
        this.signature = signature;
        this.publicKey = publicKey;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String getData() {
        return text;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public int compareTo(WorkloadItem o) {
        return id.compareTo(o.getId());
    }

    @Override
    public String toString() {
        return "#" + id + ": " + text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id) &&
                Objects.equals(text, message.text) &&
                Arrays.equals(signature, message.signature) &&
                Objects.equals(publicKey, message.publicKey);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, text, publicKey);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
}
