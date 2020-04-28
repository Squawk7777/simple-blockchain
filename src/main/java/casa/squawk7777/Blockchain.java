package casa.squawk7777;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class Blockchain implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(Blockchain.class);
    private transient Consumer<Blockchain> onAddBlockEventListener;

    private List<Block> chain;
    private Integer complexity;
    private String seekingAlNum = "0";

    public Blockchain(Integer complexity) {
        log.debug("Initializing blockchain with complexity level: {}", complexity);
        this.complexity = complexity;
        this.chain = new ArrayList<>();
    }

    public void addBlock(String data) {
        log.info("Adding new block...");
        Block block = generateBlock(data);
        log.debug("New Block generated with ID: {} ({})", block.getId(), block.getCurrentHash());

        this.chain.add(block);

        if (Objects.nonNull(this.onAddBlockEventListener)) {
            this.onAddBlockEventListener.accept(this);
        }
    }

    private Block generateBlock(String data) {
        String seekingString = seekingAlNum.repeat(complexity);
        log.debug("Seeking for hash starting with: {}", seekingString);

        String currentHash = "";
        String previousHash = getLastHash();
        long salt = 0L;

        long startTime = System.currentTimeMillis();
        while (!currentHash.startsWith(seekingString)) {
            salt = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
            currentHash = BlockchainUtil.calculateHash(data + salt + previousHash);
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("Found appropriate hash ({}) with salt: {}", currentHash, salt);

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

    public Integer getSize() {
        return chain.size();
    }

    public void printChain() {
        String output = chain.stream()
                .map(Block::toString)
                .collect(Collectors.joining("\n\n"));

        System.out.println(output);
    }

    private void verifyBlock(Block block) {
        String calculatedHash = BlockchainUtil.calculateHash(block.getData() + block.getSalt() + block.getPreviousHash());
        log.debug("Verifying block. Newly calculated / original block hash:\n{}\n{}", calculatedHash, block.getCurrentHash());

        if (!calculatedHash.equals(block.getCurrentHash())) {
            throw new RuntimeException("...");
        }
    }
}
