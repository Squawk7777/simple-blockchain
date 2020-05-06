package casa.squawk7777;

import casa.squawk7777.exceptions.BlockchainException;
import casa.squawk7777.exceptions.ChallengeExpiredException;
import casa.squawk7777.exceptions.InvalidBlockException;
import casa.squawk7777.exceptions.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;

public class Miner {
    private static final Logger log = LoggerFactory.getLogger(Miner.class);

    private final Blockchain blockchain;
    private final String minerTitle;
    private final KeyPair keyPair;

    public Miner(Blockchain blockchain, String minerTitle) {
        this.blockchain = blockchain;
        this.minerTitle = minerTitle;
        this.keyPair = SecurityUtil.generateKeyPair();
    }

    public String getTitle() {
        return minerTitle;
    }

    public void generateBlock() {
        try {
            Block block = BlockchainUtil.generateBlock(
                    blockchain.getChallenge(Miner.this), Thread.currentThread().getName());
            blockchain.offerBlock(block);
        } catch (BlockchainException | TransactionException | InvalidBlockException e) {
            log.debug("Unable to generate block: {}", e.getMessage(), e);
        } catch (ChallengeExpiredException e) {
            log.debug("Challenge expired: {}", e.getMessage());
        }
    }

    protected KeyPair getKeys() {
        return keyPair;
    }
}
