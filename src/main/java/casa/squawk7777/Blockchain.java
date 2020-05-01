package casa.squawk7777;

import casa.squawk7777.exceptions.BlockchainException;
import casa.squawk7777.exceptions.CorruptedChainException;
import casa.squawk7777.exceptions.InvalidBlockException;
import casa.squawk7777.exceptions.InvalidSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


public class Blockchain<T extends WorkloadItem> {
    private static final Logger log = LoggerFactory.getLogger(Blockchain.class);
    private static final Long MIN_TIME_GAP = 5000L;
    private static final Long MAX_TIME_GAP = 30000L;
    private static final Integer CHAIN_CAPACITY = 10;
    private static final Integer MAX_COMPLEXITY = 6;
    private static final Integer INITIAL_COMPLEXITY = 1;

    private final WorkloadSupplier<T> supplier;
    private final List<Block<T>> chain;
    private final AtomicInteger complexity;
    private final String seekingAlNum = "0";
    private final ReentrantLock offeringLock;

    private Long lastBlockTime;
    private Challenge<T> challenge;

    public Blockchain(WorkloadSupplier<T> workloadSupplier) {
        log.debug("Initializing blockchain with initial complexity at: {}", INITIAL_COMPLEXITY);
        supplier = workloadSupplier;
        complexity = new AtomicInteger(INITIAL_COMPLEXITY);
        chain = new ArrayList<>();
        lastBlockTime = System.currentTimeMillis();
        offeringLock = new ReentrantLock(true);
        challenge = new Challenge<>(1, "0", complexity.get(), getSeekingString(), new HashSet<>());
    }

    public void offerBlock(Block<T> block) throws InvalidBlockException, BlockchainException {
        if (isClosed()) {
            throw new BlockchainException("Cannot accept new block. Blockchain is full");
        }

        offeringLock.lock();
        try {
            verifyOfferedBlock(block);
            adjustComplexity();

            log.info("Adding new block #{} with hash: {}", block.getId(), block.getHash());
            chain.add(block);

            refreshChallenge();
        } finally {
            offeringLock.unlock();
        }
    }

    public boolean isClosed() {
        return chain.size() >= CHAIN_CAPACITY;
    }

    public Integer getComplexity() {
        return complexity.get();
    }

    public String getSeekingString() {
        return seekingAlNum.repeat(complexity.get());
    }

    public Block<T> getLastBlock() {
        if (chain.isEmpty()) {
            return new Block<>(0, "0");
        }
        return chain.get(chain.size() - 1);
    }

    public Block<T> getBlockById(Integer id) {
        if (id.equals(0)) {
            return new Block<>(0, "0");
        }
        return chain.get(id - 1);
    }

    public Integer getSize() {
        return chain.size();
    }

    public Challenge<T> getChallenge() throws BlockchainException {
        if (isClosed()) {
            throw new BlockchainException("All challenges done. Blockchain is full");
        }
        return challenge;
    }

    public void refreshChallenge() {
        challenge.setCompleted();
        Block<T> lastBlock = getLastBlock();
        Set<T> workload = supplier.fetchWorkload();
        challenge = new Challenge<>(lastBlock.getId() + 1, lastBlock.getHash(), getComplexity(), getSeekingString(), workload);
    }

    /**
     * Checks offered block for compliance to required complexity and valid ID
     */
    private void verifyOfferedBlock(Block<T> block) throws InvalidBlockException {
        log.debug("Verifying offered block with hash: {} ", block.getHash());
        Block<T> lastBlock = getLastBlock();

        Integer expectedId = lastBlock.getId() + 1;
        if (!block.getId().equals(expectedId)) {
            log.error("Block rejected. ID ({}) differs from expected ({})", block.getId(), expectedId);
            throw new InvalidBlockException(TextConstants.HAS_INVALID_ID);
        }

        String seekingString = getSeekingString();
        if (!block.getHash().startsWith(seekingString)) {
            log.error("Block rejected. Hash ({}) not starts with: {}", block.getHash(), seekingString);
            throw new InvalidBlockException(TextConstants.NOT_MEET_COMPLEXITY);
        }

        verifyBlock(block);
    }

    /**
     * Verifies consistency of any block (resided in the chain or just offered to)
     */
    private void verifyBlock(Block<T> block) throws InvalidBlockException {
        Block<T> previousBlock = getBlockById(block.getId() - 1);
        String calculatedHash = BlockchainUtil.calculateHash(block.getData().hashCode() + block.getComplexity() + block.getSalt() + previousBlock.getHash());

        if (!block.getHash().equals(calculatedHash)) {
            log.error("Block rejected. Hash ({}) differs from calculated ({})", block.getHash(), calculatedHash);
            throw new InvalidBlockException(TextConstants.HASH_DIFFERS_FROM_CALCULATED);
        }

        verifyWorkload(block);
    }

    /**
     * Verifies block's workload by comparing lowest workload ID in it with highest
     * workload ID from the previous block in the chain to prevent dupe-hack.
     */
    public void verifyWorkload(Block<T> block) throws InvalidBlockException {
        Set<T> workload = block.getData();

        if (!workload.isEmpty()) {
            int lowestCurrentWorkloadId = workload.stream()
                    .mapToInt(WorkloadItem::getId)
                    .min()
                    .orElseThrow(() -> new InvalidBlockException("Cannot determine lowest workload ID in this block"));

            Block<T> previousBlock = getBlockById(block.getId() - 1);

            while (previousBlock.getData().isEmpty() && !previousBlock.getId().equals(1)) {
                previousBlock = getBlockById(previousBlock.getId() - 1);
            }

            int highestPreviousWorkloadId = previousBlock.getData().stream()
                    .mapToInt(WorkloadItem::getId)
                    .max()
                    .orElse(0);

            if (lowestCurrentWorkloadId <= highestPreviousWorkloadId) {
                throw new InvalidBlockException("Lowest Workload ID of currently verified block is lower or equals to highest workload ID of previous block in blockchain");
            }

            for (WorkloadItem workloadItem : workload) {
                try {
                    SecurityHelper.verifySignature(workloadItem.getId() + workloadItem.getData(),
                            workloadItem.getSignature(),
                            workloadItem.getPublicKey());
                } catch (InvalidSignatureException e) {
                    throw new InvalidBlockException("Workload item's signature is invalid: " + e.getMessage(), e);
                } catch (GeneralSecurityException e) {
                    throw new InvalidBlockException("Unable to verify workload item's signature: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Verifies all the blocks in the chain
     */
    public void verifyChain() throws CorruptedChainException {
        try {
            for (Block<T> block : chain) {
                verifyBlock(block);
            }
        } catch (InvalidBlockException e) {
            throw new CorruptedChainException(e.getMessage(), e);
        }
    }

    /**
     * Adjusts complexity value according to time gap between last added and currently offered blocks
     */
    private void adjustComplexity() {
        long currentTimeGap = System.currentTimeMillis() - lastBlockTime;
        log.debug("Time gap from last added block: {} seconds (complexity: {})", (currentTimeGap / 1000), complexity.get());
        if (currentTimeGap < MIN_TIME_GAP && complexity.get() < MAX_COMPLEXITY) {
            complexity.incrementAndGet();
        } else if (currentTimeGap > MAX_TIME_GAP && complexity.get() > 0) {
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

    public class Challenge<T extends WorkloadItem> {
        private final Integer id;
        private volatile boolean isCompleted;
        private final String lastHash;
        private final Integer complexity;
        private final String seekingString;
        private final Set<T> workload;

        private Challenge(Integer id, String lastHash, Integer complexity, String seekingString, Set<T> workload) {
            this.id = id;
            this.isCompleted = false;
            this.lastHash = lastHash;
            this.complexity = complexity;
            this.seekingString = seekingString;
            this.workload = workload;
        }

        public Integer getId() {
            return id;
        }

        public String getLastHash() {
            return lastHash;
        }

        public Integer getComplexity() {
            return complexity;
        }

        public String getSeekingString() {
            return seekingString;
        }

        public Set<T> getWorkload() {
            return workload;
        }

        public Integer getLastWorkloadItemId() {
            return workload.stream()
                    .mapToInt(WorkloadItem::getId)
                    .max()
                    .orElse(0);
        }

        public boolean isCompleted() {
            return isCompleted;
        }

        private void setCompleted() {
            isCompleted = true;
        }
    }
}
