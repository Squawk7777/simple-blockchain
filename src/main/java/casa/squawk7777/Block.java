package casa.squawk7777;

import java.io.Serializable;

public class Block implements Serializable {
    private Integer id;
    private Integer complexity;
    private Long salt;
    private String hash;
    private String miner;
    private String data;

    public Block(Integer id, String hash) {
        this.id = id;
        this.hash = hash;
    }

    public Block(Integer id, Integer complexity, Long salt, String hash, String miner, String data) {
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

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Block ID: " + id +
                "\nComplexity: " + complexity +
                "\nSalt: " + salt +
                "\nMined by: " + miner +
                "\nCurrent block hash:\n" + hash;
    }
}
