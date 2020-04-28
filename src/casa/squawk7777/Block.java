package casa.squawk7777;

import java.io.Serializable;
import java.util.Date;

public class Block implements Serializable {
    private Integer id;
    private Long salt;
    private Long timeStamp;
    private Long timeSpent;
    private String currentHash;
    private String previousHash;
    private String data;

    public Block(Integer id, Long salt, Long timeSpent, String currentHash, String previousHash, String data) {
        this.id = id;
        this.salt = salt;
        this.timeSpent = timeSpent;
        this.currentHash = currentHash;
        this.previousHash = previousHash;
        this.data = data;

        this.timeStamp = new Date().getTime();
    }

    public Integer getId() {
        return id;
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
                "\nSalt: " + salt +
                "\nTimestamp: " + timeStamp +
                "\nCurrent block hash:\n" + currentHash +
                "\nPrevious block hash:\n" + previousHash +
                "\nGeneration time: " + (timeSpent / 1000) + " seconds";
    }
}
