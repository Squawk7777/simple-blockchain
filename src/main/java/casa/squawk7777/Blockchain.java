package casa.squawk7777;

import casa.squawk7777.exceptions.CorruptedChainException;
import casa.squawk7777.exceptions.InvalidBlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class Blockchain implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(Blockchain.class);
    private transient Consumer<Blockchain> onAddBlockEventListener;

    private List<Block> chain;
    private volatile AtomicInteger complexity;
    private final String SEEKING_ALNUM = "0";

    private Long lastBlockTime;

    private final Long MIN_TIME_GAP = 5000L;
    private final Long MAX_TIME_GAP = 30000L;


    public Blockchain(Integer initialComplexity) {
        log.debug("Initializing blockchain with initial complexity at: {}", initialComplexity);
        this.complexity = new AtomicInteger(initialComplexity);
        this.chain = new ArrayList<>();
        this.lastBlockTime = System.currentTimeMillis();
    }

    synchronized public void offerBlock(Block block) throws InvalidBlockException {
        verifyOfferedBlock(block);
        adjustComplexity();

        log.info("Adding new block #{} with hash: {}", block.getId(), block.getCurrentHash());
        this.chain.add(block);

        if (Objects.nonNull(this.onAddBlockEventListener)) {
            this.onAddBlockEventListener.accept(this);
        }
    }

    public Integer getComplexity() {
        return complexity.get();
    }

    public String getSeekingString() {
        return SEEKING_ALNUM.repeat(complexity.get());
    }

    public Block getLastBlock() {
        if (chain.isEmpty()) {
            return new Block(0, "0");
        }
        return chain.get(chain.size() - 1);
    }

    public void setOnAddBlockEventListener(Consumer<Blockchain> onAddBlockEventListener) {
        this.onAddBlockEventListener = onAddBlockEventListener;
    }

    public Integer getSize() {
        return chain.size();
    }

    private void verifyOfferedBlock(Block block) throws InvalidBlockException {
        log.debug("Verifying offered block with hash: {} ", block.getCurrentHash());
        Block lastBlock = getLastBlock();

        Integer expectedId = lastBlock.getId() + 1;
        if (!block.getId().equals(expectedId)) {
            log.error("Block rejected. ID ({}) differs from expected ({})", block.getId(), expectedId);
            throw new InvalidBlockException(TextConstants.HAS_INVALID_ID);
        }

        String previousBlockHash = lastBlock.getCurrentHash();
        if (!block.getPreviousHash().equals(previousBlockHash)) {
            log.error("Block rejected. Previous block's hash ({}) differs from expected ({}))", block.getPreviousHash(), previousBlockHash);
            throw new InvalidBlockException(TextConstants.PREVIOUS_HASH_IS_INVALID);
        }

        String seekingString = getSeekingString();
        if (!block.getCurrentHash().startsWith(seekingString)) {
            log.error("Block rejected. Hash ({}) not starts with: {}", block.getCurrentHash(), seekingString);
            throw new InvalidBlockException(TextConstants.NOT_MEET_COMPLEXITY);
        }

        verifyBlockHash(block);
    }

    private void verifyBlockHash(Block block) throws InvalidBlockException {
        String calculatedHash = BlockchainUtil.calculateHash(block.getData() + block.getComplexity() + block.getSalt() + block.getPreviousHash());
        log.debug("Verifying block. Newly calculated / original block hash:\n{}\n{}", calculatedHash, block.getCurrentHash());

        if (!block.getCurrentHash().equals(calculatedHash)) {
            log.error("Block rejected. Hash ({}) differs from calculated ({})", block.getCurrentHash(), calculatedHash);
            throw new InvalidBlockException(TextConstants.HASH_DIFFERS_FROM_CALCULATED);
        }
    }

    public void verifyChain() throws CorruptedChainException {
        try {
            for (Block block : chain) {
                verifyBlockHash(block);
            }
        } catch (InvalidBlockException e) {
            throw new CorruptedChainException(e.getMessage(), e);
        }
    }

    private void adjustComplexity() {
        long currentTimeGap = System.currentTimeMillis() - lastBlockTime;
        log.debug("Time gap from last added block: {} seconds (complexity: {})", (currentTimeGap / 1000), complexity.get());
        if (currentTimeGap < MIN_TIME_GAP) {
            complexity.incrementAndGet();
        } else if (currentTimeGap > MAX_TIME_GAP) {
            complexity.decrementAndGet();
        }
        log.debug("Complexity adjusted to: {}", complexity.get());
        lastBlockTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return chain.stream()
                .map(Block::toString)
                .collect(Collectors.joining("\n\n"));
    }
}
