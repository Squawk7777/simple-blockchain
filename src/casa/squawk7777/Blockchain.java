package casa.squawk7777;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Blockchain implements Serializable {
    private transient Consumer<Blockchain> onAddBlockEventListener;
    private List<Block> chain;
    private Integer complexity;
    private String seekingAlNum = "0";

    public Blockchain(Integer complexity) {
        this.complexity = complexity;
        this.chain = new ArrayList<>();
    }

    public void addBlock(String data) {
        Block block = generateBlock(data);

        this.chain.add(block);
        this.onAddBlockEventListener.accept(this);
    }

    private Block generateBlock(String data) {
        String seekingString = seekingAlNum.repeat(complexity);
        String currentHash = "";
        String previousHash = getLastHash();
        long salt = 0L;

        long startTime = System.currentTimeMillis();
        while (!currentHash.startsWith(seekingString)) {
            salt = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
            currentHash = BlockchainUtil.calculateHash(data + salt + previousHash);
        }
        long elapsedTime = System.currentTimeMillis() - startTime;

        return new Block(getLastId() + 1, salt, elapsedTime, currentHash, previousHash, data);
    }

    public Integer getLastId() {
        return chain.isEmpty() ? 0 : chain.get(chain.size() - 1).getId();
    }

    public String getLastHash() {
        return chain.isEmpty() ? "0" : chain.get(chain.size() - 1).getCurrentHash();
    }

    public void setOnAddBlockEventListener(Consumer<Blockchain> onAddBlockEventListener) {
        this.onAddBlockEventListener = onAddBlockEventListener;
    }

    public void printChain() {
        String output = chain.stream()
                .map(Block::toString)
                .collect(Collectors.joining("\n\n"));

        System.out.println(output);
    }

    private void verifyBlock(Block block) {
        String calculatedHash = BlockchainUtil.calculateHash(block.getData() + block.getSalt() + block.getPreviousHash());

        if (!calculatedHash.equals(block.getCurrentHash())) {
            throw new RuntimeException("...");
        }
    }
}
