package casa.squawk7777;

import casa.squawk7777.exceptions.InvalidSignatureException;
import casa.squawk7777.exceptions.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class MinerHelper {
    private static final Logger log = LoggerFactory.getLogger(MinerHelper.class);
    private static volatile MinerHelper instance;
    private final List<Miner> minerPool;

    public MinerHelper() {
        minerPool = Collections.synchronizedList(new ArrayList<>());
    }

    public static MinerHelper getInstance() {
        MinerHelper localInstance = instance;
        if (Objects.isNull(localInstance)) {
            synchronized (MinerHelper.class) {
                localInstance = instance;
                if (Objects.isNull(localInstance)) {
                    instance = localInstance = new MinerHelper();
                }
            }
        }
        return localInstance;
    }

    public void registerMiner(Miner miner) {
        minerPool.add(miner);
    }

    public void offerRandomTransaction(Blockchain blockchain) throws GeneralSecurityException, TransactionException, InterruptedException, InvalidSignatureException {
        if (!blockchain.isClosed()) {
            double creditBalance;
            Miner richMiner;
            int maxRetries = 10;

            do {
                richMiner = getRandomMiner();
            } while ((creditBalance = blockchain.getEstimatedBalance(richMiner)) <= 0 && --maxRetries > 0);

            if (creditBalance > 0) {

                log.debug("Proofed balance {} = {}", richMiner.getTitle(), creditBalance);

                Miner poorMiner;
                do {
                    poorMiner = getRandomMiner();
                } while (poorMiner.equals(richMiner));

                Integer transactionId = blockchain.getNextTransactionId();
                Transaction transaction = new Transaction(transactionId,
                        richMiner,
                        poorMiner,
                        (long) creditBalance);
                SecurityUtil.sign(transaction, richMiner.getKeys());
                log.debug("New Transaction: {} -> {} {}", transaction.getSender().getTitle(), transaction.getRecipient().getTitle(), transaction.getAmount());

                blockchain.offerTransaction(transaction);
            }
        }
    }

    public String getBalances(Blockchain blockchain) {
        return minerPool.stream().map(m -> m.getTitle() + " = " + blockchain.getBalance(m)).collect(Collectors.joining("\n"));
    }

    private Miner getRandomMiner() {
        int rnd = ThreadLocalRandom.current().nextInt(0, minerPool.size());
        return minerPool.get(rnd);
    }
}
