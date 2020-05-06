package casa.squawk7777;

import casa.squawk7777.exceptions.BlockchainException;
import casa.squawk7777.exceptions.InvalidSignatureException;
import casa.squawk7777.exceptions.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class MinerHelper {
    private static final Logger log = LoggerFactory.getLogger(MinerHelper.class);
    private final List<Miner> minerPool;

    public MinerHelper() {
        minerPool = Collections.synchronizedList(new ArrayList<>());
    }

    public static MinerHelper getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void registerMiner(Miner miner) {
        log.debug("{} added to miners pool", miner.getTitle());
        minerPool.add(miner);
    }

    public void offerRandomTransaction(Blockchain blockchain) {
        if (!blockchain.isClosed()) {
            double balance;
            Miner richMiner;
            int maxRetries = 10;

            do {
                richMiner = getRandomMiner();
            } while ((balance = blockchain.getEstimatedBalance(richMiner)) <= 0 && --maxRetries > 0);

            if (balance > 0) {
                log.debug("Verified {}'s balance: {} coins", richMiner.getTitle(), balance);

                Miner poorMiner;
                do {
                    poorMiner = getRandomMiner();
                } while (poorMiner.equals(richMiner));

                Integer transactionId = blockchain.getNextTransactionId();
                Transaction transaction = new Transaction(transactionId,
                        richMiner,
                        poorMiner,
                        Math.round(balance / 2));

                try {
                    SecurityUtil.sign(transaction, richMiner.getKeys());
                    log.debug("New transaction: {} sent {} coins to {}",
                            transaction.getSender().getTitle(),
                            transaction.getAmount(),
                            transaction.getRecipient().getTitle());

                    blockchain.offerTransaction(transaction);
                } catch (BlockchainException | GeneralSecurityException | InvalidSignatureException | TransactionException e) {
                    log.debug("Unable to generate and offer a random transaction: {}", e.getMessage(), e);
                }
            }
        }
    }

    public String getBalances(Blockchain blockchain) {
        return minerPool.stream()
                .map(m -> m.getTitle() + " = " + blockchain.getConfirmedBalance(m))
                .collect(Collectors.joining("\n"));
    }

    private Miner getRandomMiner() {
        int rnd = ThreadLocalRandom.current().nextInt(0, minerPool.size());
        return minerPool.get(rnd);
    }

    private static class InstanceHolder {
        public static final MinerHelper INSTANCE = new MinerHelper();
    }
}
