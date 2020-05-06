package casa.squawk7777;

import casa.squawk7777.exceptions.BlockchainException;
import casa.squawk7777.exceptions.InvalidBlockException;
import casa.squawk7777.exceptions.InvalidSignatureException;
import casa.squawk7777.exceptions.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Blockchain {
    public static final String TRANSACTION_ID_IS_TOO_LOW = "Transaction ID is equals or lower than highest existing transaction ID";
    private static final Logger log = LoggerFactory.getLogger(Blockchain.class);
    private static final long MIN_TIME_GAP = 5000L;
    private static final long MAX_TIME_GAP = 30000L;
    private static final int MAX_COMPLEXITY = 5;
    private static final int REWARD_AMOUNT = 100;
    private static final int INITIAL_COMPLEXITY = 3;
    private static final String SEEKING_AL_NUM_CHAR = "0";
    private static final String BLOCKCHAIN_OWNER_TITLE = "BLOCKCHAIN";

    private final Miner chainOwner;
    private final List<Block> chain;
    private final Set<Transaction> transactionPool;
    private final AtomicInteger complexity;
    private final AtomicInteger lastTransactionId;
    private final StampedLock stateLock;
    private final int chainCapacity;

    private volatile AtomicBoolean isChallengeDone;     // we have to be sure that reference is always fresh
    private volatile boolean isClosed;

    private long lastBlockTime;
    private Consumer<Blockchain> onCloseEventHandler;

    public Blockchain(int chainCapacity) {
        this.chainCapacity = chainCapacity;
        this.chainOwner = new Miner(this, BLOCKCHAIN_OWNER_TITLE);
        this.chain = new ArrayList<>();
        this.transactionPool = Collections.synchronizedSet(new TreeSet<>());
        this.complexity = new AtomicInteger(INITIAL_COMPLEXITY);
        this.lastTransactionId = new AtomicInteger(0);
        this.stateLock = new StampedLock();
        this.isChallengeDone = new AtomicBoolean(false);
        this.lastBlockTime = System.currentTimeMillis();
    }

    public void offerTransaction(Transaction transaction) throws TransactionException, GeneralSecurityException, InvalidSignatureException, BlockchainException {
        SecurityUtil.verifySignature(transaction);

        long stamp = stateLock.writeLock();
        try {
            if (isClosed) {
                throw new BlockchainException(TextConstants.BLOCKCHAIN_CLOSED);
            }

            if (transactionPool.stream().anyMatch(t -> t.getId().equals(transaction.getId()))) {
                throw new TransactionException(TextConstants.TRANSACTION_ALREADY_EXIST);
            }

            if (transaction.getId() <= getHighestTransactionId()) {
                throw new TransactionException(TRANSACTION_ID_IS_TOO_LOW);
            }

            if (transaction.getAmount() > getEstimatedBalance(transaction.getSender())) {
                throw new TransactionException(TextConstants.SENDER_IS_SHORT_ON_FUNDS);
            }
            log.debug("Transaction accepted to pool: {}", transaction);

            transactionPool.add(transaction);
        } finally {
            stateLock.unlockWrite(stamp);
        }
    }

    public void offerBlock(Block block) throws InvalidBlockException, BlockchainException, TransactionException {
        long stamp = stateLock.writeLock();
        try {
            if (isClosed) {
                throw new BlockchainException(TextConstants.BLOCKCHAIN_CLOSED);
            }

            verifyOfferedBlock(block);
            log.info("Adding new block #{} with hash: {}", block.getId(), block.getHash());
            chain.add(block);

            adjustComplexity();
            transactionPool.removeAll(block.getTransactions());
            isChallengeDone.set(true);
            isChallengeDone = new AtomicBoolean(false);
            checkCapacityLimit();
        } finally {
            stateLock.unlockWrite(stamp);
        }
    }

    private Transaction getRewardTransaction(Miner miner) {
        log.debug("{} rewarded for block generation (confirmed vs estimated balance: {} | {})",
                miner.getTitle(),
                getConfirmedBalance(miner),
                getEstimatedBalance(miner));
        return new Transaction(getNextTransactionId(), chainOwner, miner, REWARD_AMOUNT);
    }

    public Challenge getChallenge(Miner miner) throws BlockchainException {
        long stamp = stateLock.tryOptimisticRead();

        Block lastBlock = getLastBlock();
        AtomicBoolean challengeDoneRef = isChallengeDone;
        int complexityValue = complexity.get();
        TreeSet<Transaction> transactions = new TreeSet<>(transactionPool);

        if (isClosed) {
            throw new BlockchainException(TextConstants.BLOCKCHAIN_CLOSED);
        }

        if (!stateLock.validate(stamp)) {
            stamp = stateLock.readLock();
            try {
                if (isClosed) {
                    throw new BlockchainException(TextConstants.BLOCKCHAIN_CLOSED);
                }
                lastBlock = getLastBlock();
                challengeDoneRef = isChallengeDone;
                complexityValue = complexity.get();
                transactions = new TreeSet<>(transactionPool);
            } finally {
                stateLock.unlockRead(stamp);
            }
        }

        Transaction rewardTransaction = getRewardTransaction(miner);
        transactions.add(rewardTransaction);

        return new Challenge(lastBlock.getId() + 1,
                lastBlock.getHash(),
                complexityValue,
                getSeekingString(),
                transactions,
                challengeDoneRef);
    }

    /**
     * Checks offered block for compliance to required complexity and valid ID
     */
    private void verifyOfferedBlock(Block block) throws InvalidBlockException, TransactionException, BlockchainException {
        log.debug("Verifying offered block ID {} with {} transactions", block.getId(), block.getTransactions().size());
        Block lastBlock = getLastBlock();

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

        for (Transaction transaction : block.getTransactions()) {
            if (!transactionPool.contains(transaction) && !transaction.getSender().equals(chainOwner)) {
                log.error("Transaction ID {} is not present in the pool", transaction.getId());
                throw new TransactionException(TextConstants.NOT_PRESENT_IN_THE_POOL);
            }
        }

        verifyBlock(block);
    }

    /**
     * Verifies consistency of any block (resided in the chain or just offered to)
     */
    private void verifyBlock(Block block) throws InvalidBlockException, BlockchainException {
        Block previousBlock = getBlockById(block.getId() - 1);
        String calculatedHash = BlockchainUtil.calculateHash(
                block.getTransactions().hashCode() + block.getComplexity() + block.getNonce() + previousBlock.getHash());

        if (!block.getHash().equals(calculatedHash)) {
            log.error("Block rejected. Hash ({}) differs from calculated ({})", block.getHash(), calculatedHash);
            throw new InvalidBlockException(TextConstants.HASH_DIFFERS_FROM_CALCULATED);
        }
    }

    /**
     * Verifies all the blocks in the chain
     */
    public void verifyChain() throws BlockchainException {
        try {
            for (Block block : chain) {
                verifyBlock(block);
            }
        } catch (InvalidBlockException e) {
            throw new BlockchainException(e.getMessage(), e);
        }
    }

    /**
     * Calculates Miner's balance based on data inside blockchain together with pending transactions in the pool
     */
    public long getEstimatedBalance(Miner miner) {
        return getConfirmedBalance(miner) + getTransactionsBalance(transactionPool.stream(), miner);
    }

    public long getConfirmedBalance(Miner miner) {
        return getTransactionsBalance(chain.stream()
                        .flatMap(b -> b.getTransactions().stream()),
                miner);
    }

    private long getTransactionsBalance(Stream<Transaction> transactionStream, Miner miner) {
        return transactionStream.mapToLong(t -> {
            if (t.getSender().equals(miner)) {
                return -(t.getAmount());
            } else if (t.getRecipient().equals(miner)) {
                return t.getAmount();
            }
            return 0;
        }).sum();
    }

    public String getSeekingString() {
        return SEEKING_AL_NUM_CHAR.repeat(complexity.get());
    }

    public Block getLastBlock() {
        if (chain.isEmpty()) {
            return getBlockById(0);
        }
        return chain.get(chain.size() - 1);
    }

    public Block getBlockById(Integer id) {
        if (id.equals(0)) {
            return new Block(0, "0");
        }
        return chain.get(id - 1);
    }

    public Integer getNextTransactionId() {
        return lastTransactionId.incrementAndGet();
    }

    /**
     * Finds highest workload in blocks resides in blockchain
     *
     * @return Integer highest workload id or 0 if no workload is present
     */
    private Integer getHighestTransactionId() {
        Block block = getLastBlock();
        int blockId = block.getId();

        while (block.getTransactions().isEmpty() && !block.getId().equals(1)) {
            block = getBlockById(--blockId);
        }

        return block.getTransactions().stream()
                .mapToInt(Transaction::getId)
                .max()
                .orElse(0);
    }

    /**
     * Adjusts complexity value according to time gap between last added and currently offered blocks
     */
    private void adjustComplexity() {
        long currentTimeGap = System.currentTimeMillis() - lastBlockTime;
        log.debug("Time gap after the last added block: {} seconds (complexity: {})", (currentTimeGap / 1000), complexity.get());

        if (currentTimeGap < MIN_TIME_GAP && complexity.get() < MAX_COMPLEXITY) {
            complexity.incrementAndGet();
        } else if (currentTimeGap > MAX_TIME_GAP && complexity.get() > 0) {
            complexity.decrementAndGet();
        }
        log.debug("Complexity adjusted to: {}", complexity.get());
        lastBlockTime = System.currentTimeMillis();
    }

    public void checkCapacityLimit() {
        if (chain.size() >= chainCapacity) {
            isClosed = true;
            log.debug("Blockchain full & closed. Calling on onCloseEventHandler...");
            onCloseEventHandler.accept(this);
        }
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setOnCloseEventHandler(Consumer<Blockchain> onCloseEventHandler) {
        this.onCloseEventHandler = onCloseEventHandler;
    }

    @Override
    public String toString() {
        return chain.stream()
                .map(Block::toString)
                .collect(Collectors.joining("\n\n"));
    }

    public static class Challenge {
        private final Integer nextBlockId;
        private final String lastHash;
        private final int complexity;
        private final String seekingString;
        private final Set<Transaction> transactions;
        private final AtomicBoolean isDone;

        private Challenge(Integer nextBlockId, String lastHash, int complexity, String seekingString, Set<Transaction> transactions, AtomicBoolean isChallengeDone) {
            this.nextBlockId = nextBlockId;
            this.lastHash = lastHash;
            this.complexity = complexity;
            this.seekingString = seekingString;
            this.transactions = transactions;
            this.isDone = isChallengeDone;
        }

        public Integer getNextBlockId() {
            return nextBlockId;
        }

        public String getLastHash() {
            return lastHash;
        }

        public int getComplexity() {
            return complexity;
        }

        public String getSeekingString() {
            return seekingString;
        }

        public Set<Transaction> getTransactions() {
            return transactions;
        }

        public boolean isDone() {
            return isDone.get();
        }
    }
}
