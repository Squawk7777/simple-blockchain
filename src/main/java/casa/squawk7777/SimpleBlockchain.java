package casa.squawk7777;

import casa.squawk7777.exceptions.CorruptedChainException;
import casa.squawk7777.exceptions.InvalidBlockException;

import java.io.File;
import java.util.Scanner;

public class SimpleBlockchain {
    private static final File BLOCKCHAIN_FILE = new File("blockchain.data");

    public static void main(String[] args) {
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

        //blockchain.setOnAddBlockEventListener(b ->
        //        BlockchainUtil.saveBlockchain(BLOCKCHAIN_FILE, b));


        try {
            blockchain.offerBlock(BlockchainUtil.generateBlock(blockchain, "M1", "a"));
            blockchain.offerBlock(BlockchainUtil.generateBlock(blockchain, "M1", "b"));
            blockchain.offerBlock(BlockchainUtil.generateBlock(blockchain, "M1", "c"));
            blockchain.offerBlock(BlockchainUtil.generateBlock(blockchain, "M1", "d"));
            blockchain.offerBlock(BlockchainUtil.generateBlock(blockchain, "M1", "e"));

            blockchain.verifyChain();
        } catch (InvalidBlockException | CorruptedChainException e) {
            System.out.println(e.getMessage());
        }



        System.out.println(blockchain);
    }
}
