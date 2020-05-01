package casa.squawk7777;

import casa.squawk7777.exceptions.CorruptedChainException;
import com.github.javafaker.Faker;

import java.io.File;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class SimpleBlockchain {
    private static final File BLOCKCHAIN_FILE = new File("blockchain.data");

    private static final Integer MINERS_NUMBER = 10;
    private static final Integer CHATTERS_NUMBER = 5;

    public static void main(String[] args) throws InterruptedException {
        Chat chat = new Chat();

        Blockchain<Message> blockchain = new Blockchain<>(chat);

        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

        Faker faker = new Faker(new Random());

        IntStream.range(0, CHATTERS_NUMBER)
                .forEach(i -> forkJoinPool.submit(new Chatter(SecurityHelper.generateKeyPair(), chat, faker, 6)));

        IntStream.range(0, MINERS_NUMBER)
                .forEach(i -> forkJoinPool.submit(new Miner(blockchain, faker.funnyName().name())));

        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(600L, TimeUnit.SECONDS);

        try {
            blockchain.verifyChain();
        } catch (CorruptedChainException e) {
            e.printStackTrace();
        }

        System.out.println(blockchain);
    }
}
