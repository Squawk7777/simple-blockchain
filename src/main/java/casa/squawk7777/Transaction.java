package casa.squawk7777;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;

public class Transaction implements Signable, Comparable<Transaction> {
    private final Integer id;
    private final Miner sender;
    private final Miner recipient;
    private final long amount;
    private PublicKey publicKey;
    private byte[] signature;

    public Transaction(Integer id, Miner sender, Miner recipient, long amount) {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
    }

    public Integer getId() {
        return id;
    }

    public Miner getSender() {
        return sender;
    }

    public Miner getRecipient() {
        return recipient;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public void sign(byte[] signature, PublicKey publicKey) {
        this.publicKey = publicKey;
        this.signature = signature;
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
    public String getDigest() {
        return "" + id + sender.hashCode() + recipient.hashCode() + amount;
    }

    @Override
    public int compareTo(Transaction o) {
        return id.compareTo(o.getId());
    }

    @Override
    public String toString() {
        return String.format("#%d: %s => %s, %d", id, sender.getTitle(), recipient.getTitle(), amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return amount == that.amount &&
                id.equals(that.id) &&
                sender.equals(that.sender) &&
                recipient.equals(that.recipient) &&
                Objects.equals(publicKey, that.publicKey) &&
                Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, sender, recipient, amount, publicKey);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
}
