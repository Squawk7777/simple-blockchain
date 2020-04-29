package casa.squawk7777;

import casa.squawk7777.exceptions.CorruptedChainException;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class SimpleBlockchain {
    private static final File BLOCKCHAIN_FILE = new File("blockchain.data");

    public static void main(String[] args) throws InterruptedException {
        Blockchain blockchain;

        if (BLOCKCHAIN_FILE.exists()) {
            blockchain = BlockchainUtil.loadBlockchain(BLOCKCHAIN_FILE);
        } else {
            int complexity;

            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("Enter the hash-seeking complexity value (1 - easiest, 10 - hardest):");

                complexity = scanner.nextInt();
            }
            blockchain = new Blockchain(complexity);
        }

        blockchain.setOnAddBlockEventListener(b ->
                BlockchainUtil.saveBlockchain(BLOCKCHAIN_FILE, b));

        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

        for (int i = 0; i < 10; i++) {
            forkJoinPool.submit(new Miner(blockchain, "M" + i, String.valueOf(i)));
        }

        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(60L, TimeUnit.SECONDS);

        try {
            blockchain.verifyChain();
        } catch (CorruptedChainException e) {
            e.printStackTrace();
        }

        System.out.println(blockchain);
    }
}
