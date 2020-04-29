package casa.squawk7777;

import casa.squawk7777.exceptions.InvalidBlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RecursiveAction;

public class Miner extends RecursiveAction {
    private final Logger log = LoggerFactory.getLogger(Miner.class);
    private final Blockchain blockchain;
    private final String miner;
    private final String data;

    public Miner(Blockchain blockchain, String miner, String data) {
        this.blockchain = blockchain;
        this.miner = miner;
        this.data = data;
    }

    @Override
    protected void compute() {
        Block block = BlockchainUtil.generateBlock(blockchain, miner, data);
        try {
            blockchain.offerBlock(block);
        } catch (InvalidBlockException e) {
            log.error("Generated invalid (possibly late) block: {}", e.getMessage());
        }
    }
}
