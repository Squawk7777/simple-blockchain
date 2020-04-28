package casa.squawk7777;

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

        blockchain.setOnAddBlockEventListener(b ->
                BlockchainUtil.saveBlockchain(BLOCKCHAIN_FILE, b));

        blockchain.addBlock("a");
        blockchain.addBlock("bb");
        blockchain.addBlock("ccc");
        blockchain.addBlock("dddd");
        blockchain.addBlock("eeeee");

        blockchain.printChain();
    }
}
