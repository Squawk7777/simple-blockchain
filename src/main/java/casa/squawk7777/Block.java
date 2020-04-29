package casa.squawk7777;

import java.io.Serializable;

public class Block implements Serializable {
    private Integer id;
    private Integer complexity;
    private Long salt;
    private String currentHash;
    private String previousHash;
    private String miner;
    private String data;

    public Block(Integer id, String currentHash) {
        this.id = id;
        this.currentHash = currentHash;
    }

    public Block(Integer id, Integer complexity, Long salt, String currentHash, String previousHash, String miner, String data) {
        this.id = id;
        this.complexity = complexity;
        this.salt = salt;
        this.currentHash = currentHash;
        this.previousHash = previousHash;
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

    public String getCurrentHash() {
        return currentHash;
    }

    public String getPreviousHash() {
        return previousHash;
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
                "\nCurrent block hash:\n" + currentHash +
                "\nPrevious block hash:\n" + previousHash;
    }
}
