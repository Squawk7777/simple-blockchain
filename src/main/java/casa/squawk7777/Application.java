package casa.squawk7777;

import casa.squawk7777.exceptions.CorruptedChainException;
import casa.squawk7777.workload.Chat;
import casa.squawk7777.workload.Chatter;
import casa.squawk7777.workload.Message;
import com.github.javafaker.Faker;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class Application {
    private static final Integer MINERS_NUMBER = 5;
    private static final Integer CHATTERS_NUMBER = 15;

    public static void main(String[] args) throws Exception {
        Chat chat = new Chat();

        Blockchain<Message> blockchain = new Blockchain<>(chat);

        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

        Faker faker = new Faker(new Random());

        IntStream.range(0, CHATTERS_NUMBER)
                .forEach(i -> forkJoinPool.submit(new Chatter(SecurityHelper.generateKeyPair(), chat, faker, 3)));

        IntStream.range(0, MINERS_NUMBER)
                .forEach(i -> forkJoinPool.submit(new Miner<>(blockchain, faker.funnyName().name())));


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
