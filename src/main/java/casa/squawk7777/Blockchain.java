package casa.squawk7777;

import casa.squawk7777.exceptions.BlockchainException;
import casa.squawk7777.exceptions.CorruptedChainException;
import casa.squawk7777.exceptions.InvalidBlockException;
import casa.squawk7777.exceptions.InvalidSignatureException;
import casa.squawk7777.exceptions.TransactionException;
import casa.squawk7777.workload.TransactionUtil;
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
    private static final Logger log = LoggerFactory.getLogger(Blockchain.class);
    private static final long MIN_TIME_GAP = 5000L;
    private static final long MAX_TIME_GAP = 30000L;
    private static final int MAX_COMPLEXITY = 5;
    private static final int REWARD_AMOUNT = 100;
    private static final int INITIAL_COMPLEXITY = 3;
    private static final String SEEKING_AL_NUM_CHAR = "0";
    private static final String BLOCKCHAIN_MINER_TITLE = "BLOCKCHAIN";

    private final Miner chainOwner;
    private final List<Block> chain;
    private final Set<Transaction> transactionPool;
    private final AtomicInteger complexity;
    private final AtomicInteger lastTransactionId;
    private final StampedLock stateLock;
    private volatile AtomicBoolean isChallengeDone;     // we have to be sure that reference is always fresh

    private long lastBlockTime;
    private int chainCapacity;
    private Consumer<Blockchain> onCloseEventHandler;

    public Blockchain(int chainCapacity) {
        this.chainCapacity = chainCapacity;
        this.chainOwner = new Miner(this, BLOCKCHAIN_MINER_TITLE);
        this.chain = new ArrayList<>();
        this.transactionPool = Collections.synchronizedSet(new TreeSet<>());
        this.complexity = new AtomicInteger(INITIAL_COMPLEXITY);
        this.lastTransactionId = new AtomicInteger(0);
        this.stateLock = new StampedLock();
        this.isChallengeDone = new AtomicBoolean(false);
        this.lastBlockTime = System.currentTimeMillis();
    }

    public void setOnCloseEventHandler(Consumer<Blockchain> onCloseEventHandler) {
        this.onCloseEventHandler = onCloseEventHandler;
    }

    public Integer getNextTransactionId() {
        return lastTransactionId.incrementAndGet();
    }

    /**
     * Calculates Miner's balance based on data inside blockchain together with pending transactions in the pool
     */
    public long getEstimatedBalance(Miner miner) {
        return getBalance(miner) + getTransactionsBalance(transactionPool.stream(), miner);
    }

    public long getBalance(Miner miner) {
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

    public void offerTransaction(Transaction transaction) throws TransactionException, GeneralSecurityException, InvalidSignatureException, InterruptedException {
        SecurityUtil.verifySignature(transaction);

        long stamp = stateLock.writeLock();
        try {
            if (transactionPool.stream().anyMatch(t -> t.getId().equals(transaction.getId()))) {
                throw new TransactionException("Transaction with such ID is already present");
            }

            if (transaction.getId() <= getHighestTransactionId()) {
                throw new TransactionException("Transaction ID is equals or lower than highest existing transaction ID");
            }

            if (transaction.getAmount() > getEstimatedBalance(transaction.getSender())) {
                throw new TransactionException("Sender doesn't have enough funds to make this transaction");
            }
            log.debug("Transaction accepted to pool: {}", transaction);

            transactionPool.add(transaction);
        } finally {
            stateLock.unlockWrite(stamp);
        }
    }

    public void offerBlock(Block block) throws InvalidBlockException, BlockchainException, TransactionException, InterruptedException {
        if (isClosed()) {
            throw new BlockchainException("Blockchain is closed");
        }

        long stamp = stateLock.writeLock();
        try {
            verifyOfferedBlock(block);
            log.info("Adding new block #{} with hash: {}", block.getId(), block.getHash());
            chain.add(block);

            adjustComplexity();
            transactionPool.removeAll(block.getTransactions());
            isChallengeDone.set(true);
            isChallengeDone = new AtomicBoolean(false);
        } finally {
            stateLock.unlockWrite(stamp);
        }

        Transaction t = block.getTransactions().stream().filter(b -> b.getSender().equals(chainOwner)).findAny().get();
        log.debug("Miner {} rewarded for block generation (proofed / estimated balance: {} / {})", t.getRecipient().getTitle(),
                getBalance(t.getRecipient()),
                getEstimatedBalance(t.getRecipient()));
    }

    public boolean isClosed() {
        boolean isClosed = chain.size() >= chainCapacity;
        if (isClosed) {
            log.debug("Blockchain is closed. Calling the onCloseEventHandler...");
            onCloseEventHandler.accept(this);
        }
        return isClosed;
    }

    public String getSeekingString() {
        return SEEKING_AL_NUM_CHAR.repeat(complexity.get());
    }

    public Block getLastBlock() {
        if (chain.isEmpty()) {
            return new Block(0, "0");
        }
        return chain.get(chain.size() - 1);
    }

    public Block getBlockById(Integer id) {
        if (id.equals(0)) {
            return new Block(0, "0");
        }
        return chain.get(id - 1);
    }

    private Transaction getRewardTransaction(Miner miner) {
        return new Transaction(getNextTransactionId(), chainOwner, miner, 100);
    }

    public Challenge getChallenge(Miner miner) throws BlockchainException, InterruptedException {
        if (isClosed()) {
            throw new BlockchainException("No new challenges available");
        }

        long stamp = stateLock.tryOptimisticRead();
        Block lastBlock = getLastBlock();
        AtomicBoolean challengeDoneRef = isChallengeDone;
        int complexityValue = complexity.get();
        TreeSet<Transaction> transactions = new TreeSet<>(transactionPool);

        if (!stateLock.validate(stamp)) {
            stamp = stateLock.readLock();
            try {
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
    private void verifyOfferedBlock(Block block) throws InvalidBlockException {
        log.debug("Verifying offered block with hash: {} ", block.getHash());
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
                throw new InvalidBlockException("At least one transaction is not present in the pool");
            }
        }

        verifyBlock(block);
    }

    /**
     * Verifies consistency of any block (resided in the chain or just offered to)
     */
    private void verifyBlock(Block block) throws InvalidBlockException {
        Block previousBlock = getBlockById(block.getId() - 1);
        String calculatedHash = BlockchainUtil.calculateHash(block.getTransactions().hashCode() + block.getComplexity() + block.getNonce() + previousBlock.getHash());

        if (!block.getHash().equals(calculatedHash)) {
            log.error("Block rejected. Hash ({}) differs from calculated ({})", block.getHash(), calculatedHash);
            throw new InvalidBlockException(TextConstants.HASH_DIFFERS_FROM_CALCULATED);
        }
    }

    /**
     * Verifies all the blocks in the chain
     */
    public void verifyChain() throws CorruptedChainException {
        try {
            for (Block block : chain) {
                verifyBlock(block);
            }
        } catch (InvalidBlockException e) {
            throw new CorruptedChainException(e.getMessage(), e);
        }
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
        return TransactionUtil.maxTransactionId(block.getTransactions());
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

    public class Challenge {
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
