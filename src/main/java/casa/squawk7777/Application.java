package casa.squawk7777;

import com.github.javafaker.Faker;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class Application {
    private static final int THREAD_NUMBER = 4;
    private static final int MINER_NUMBER = 6;
    private static final int BLOCKCHAIN_CAPACITY = 20;
    private static final long NEW_TRANSACTION_DELAY_MS = 400L;
    private static final long NEW_BLOCK_GENERATION_DELAY_MS = 1000L;

    public static void main(String[] args) throws InterruptedException {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(THREAD_NUMBER);
        Blockchain blockchain = new Blockchain(BLOCKCHAIN_CAPACITY);
        blockchain.setOnCloseEventHandler(b -> executorService.shutdown());

        Faker faker = new Faker(new Random());
        MinerHelper minerHelper = MinerHelper.getInstance();

        executorService.scheduleWithFixedDelay(
                () -> minerHelper.offerRandomTransaction(blockchain), NEW_TRANSACTION_DELAY_MS, NEW_TRANSACTION_DELAY_MS, TimeUnit.MILLISECONDS);

        IntStream.range(0, MINER_NUMBER).forEach(i -> {
            Miner miner = new Miner(blockchain, faker.funnyName().name());
            minerHelper.registerMiner(miner);
            executorService.scheduleWithFixedDelay(
                    miner::generateBlock, NEW_BLOCK_GENERATION_DELAY_MS, NEW_BLOCK_GENERATION_DELAY_MS, TimeUnit.MILLISECONDS);
        });

        executorService.awaitTermination(300, TimeUnit.SECONDS);
        System.out.println(blockchain);

        System.out.println("\nSummary:\n" + MinerHelper.getInstance().getBalances(blockchain));
    }
}
