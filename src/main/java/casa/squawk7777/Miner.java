package casa.squawk7777;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;

public class Miner {
    private static final Logger log = LoggerFactory.getLogger(Miner.class);

    private final transient Blockchain blockchain;
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
        if (!blockchain.isClosed()) {
            try {
                Block block = BlockchainUtil.generateBlock(blockchain.getChallenge(Miner.this), Thread.currentThread().getName());
                blockchain.offerBlock(block);
            } catch (Exception e) {
                log.debug(e.getMessage(), e);
            }
        }
    }

    public KeyPair getKeys() {
        return keyPair;
    }

}
