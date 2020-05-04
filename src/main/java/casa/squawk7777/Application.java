package casa.squawk7777;

import casa.squawk7777.exceptions.CorruptedChainException;
import casa.squawk7777.exceptions.InvalidSignatureException;
import casa.squawk7777.exceptions.TransactionException;
import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Application {
    private static final int THREAD_NUMBER = 4;
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws InterruptedException {
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(THREAD_NUMBER);
        Blockchain blockchain = new Blockchain();
        blockchain.setOnNewBlockEvent(b -> {
            if (blockchain.isClosed()) {
                executorService.shutdown();
                System.out.println("Shutting down...");
            }
        });

        Faker faker = new Faker(new Random());

        MinerHelper minerHelper = MinerHelper.getInstance();

        Miner m1 = new Miner(blockchain, faker.funnyName().name());
        Miner m2 = new Miner(blockchain, faker.funnyName().name());
        Miner m3 = new Miner(blockchain, faker.funnyName().name());

        minerHelper.registerMiner(m1);
        minerHelper.registerMiner(m2);
        minerHelper.registerMiner(m3);


        executorService.scheduleWithFixedDelay(() -> {
            try {
                minerHelper.offerRandomTransaction(blockchain);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (TransactionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvalidSignatureException e) {
                e.printStackTrace();
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);

        executorService.scheduleWithFixedDelay(() -> {
            m1.generateBlock();
        }, 1000, 1000, TimeUnit.MILLISECONDS);
        executorService.scheduleWithFixedDelay(() -> {
            m2.generateBlock();
        }, 1000, 1000, TimeUnit.MILLISECONDS);
        executorService.scheduleWithFixedDelay(() -> {
            m3.generateBlock();
        }, 1000, 1000, TimeUnit.MILLISECONDS);


        executorService.awaitTermination(60, TimeUnit.SECONDS);
        System.out.println(blockchain);

        System.out.println("\nSummary:\n" + MinerHelper.getInstance().getBalances(blockchain));

        try {
            blockchain.verifyChain();
        } catch (CorruptedChainException e) {
            e.printStackTrace();
        }
    }
}
