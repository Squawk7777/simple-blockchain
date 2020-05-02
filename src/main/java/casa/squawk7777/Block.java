package casa.squawk7777;

import casa.squawk7777.workload.WorkloadItem;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Block<T extends WorkloadItem> {
    private final Integer id;
    private final Integer complexity;
    private final Long salt;
    private final String hash;
    private final String miner;
    private final Set<T> data;

    public Block(Integer id, String hash) {
        this(id, 1, 0L, hash, "", new HashSet<>());
    }

    public Block(Integer id, Integer complexity, Long salt, String hash, String miner, Set<T> data) {
        this.id = id;
        this.complexity = complexity;
        this.salt = salt;
        this.hash = hash;
        this.miner = miner;
        this.data = data;
    }

    public Integer getId() {
        return id;
    }

    public Integer getComplexity() {
        return complexity;
    }

    public Long getSalt() {
        return salt;
    }

    public String getHash() {
        return hash;
    }

    public Set<T> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Block ID: " + id +
                "\nComplexity: " + complexity +
                "\nSalt: " + salt +
                "\nMined by: " + miner +
                "\nCurrent block hash:\n" + hash +
                "\nWorkload stored:\n" + data.stream()
                .map(WorkloadItem::toString)
                .collect(Collectors.joining("\n"));
    }
}
