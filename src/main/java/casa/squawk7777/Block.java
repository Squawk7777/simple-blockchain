package casa.squawk7777;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Block {
    private final Integer id;
    private final int complexity;
    private final long nonce;
    private final String hash;
    private final String miner;
    private final Set<Transaction> transactions;

    public Block(Integer id, String hash) {
        this(id, 1, 0L, hash, "", new HashSet<>());
    }

    public Block(Integer id, int complexity, long nonce, String hash, String miner, Set<Transaction> transactions) {
        this.id = id;
        this.complexity = complexity;
        this.nonce = nonce;
        this.hash = hash;
        this.miner = miner;
        this.transactions = transactions;
    }

    public Integer getId() {
        return id;
    }

    public int getComplexity() {
        return complexity;
    }

    public long getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }

    public Set<Transaction> getTransactions() {
        return transactions;
    }

    @Override
    public String toString() {
        return "Block ID: " + id +
                "\nComplexity: " + complexity +
                "\nSalt: " + nonce +
                "\nMined by: " + miner +
                "\nCurrent block hash:\n" + hash +
                "\nWorkload stored:\n" + transactions.stream()
                .map(Transaction::toString)
                .collect(Collectors.joining("\n"));
    }
}
