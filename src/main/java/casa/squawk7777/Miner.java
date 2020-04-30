package casa.squawk7777;

import casa.squawk7777.exceptions.BlockchainException;
import casa.squawk7777.exceptions.InvalidBlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RecursiveAction;

public class Miner extends RecursiveAction {
    private final Logger log = LoggerFactory.getLogger(Miner.class);
    private final Blockchain blockchain;
    private final String miner;

    public Miner(Blockchain blockchain, String miner) {
        this.blockchain = blockchain;
        this.miner = miner;
    }

    @Override
    protected void compute() {
        while (!blockchain.isClosed()) {
            try {
                Block block = BlockchainUtil.generateBlock(blockchain.getChallenge(), Thread.currentThread().getName());
                blockchain.offerBlock(block);
            } catch (InvalidBlockException e) {
                log.error("Invalid (possibly late) block: {}", e.getMessage());
            } catch (BlockchainException e) {
                log.error("Blockchain exception: {}", e.getMessage());
            }
        }

        log.debug("{}: I'm done boss!", miner);
    }
}
