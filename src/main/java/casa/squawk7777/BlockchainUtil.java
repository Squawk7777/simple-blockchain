package casa.squawk7777;

import casa.squawk7777.exceptions.InvalidBlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ThreadLocalRandom;

public class BlockchainUtil {
    private static final Logger log = LoggerFactory.getLogger(BlockchainUtil.class);
    private BlockchainUtil() {}

    public static Blockchain loadBlockchain(File blockchainFile) {
        log.debug("Loading blockchain from file: {}", blockchainFile);
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(blockchainFile))) {
            Blockchain blockchain = (Blockchain) inputStream.readObject();
            log.debug("Successfully loaded blockchain containing {} blocks", blockchain.getSize());
            return blockchain;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveBlockchain(File blockchainFile, Blockchain blockchain) {
        log.debug("Saving blockchain to file: {}", blockchainFile);
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(blockchainFile))) {
            outputStream.writeObject(blockchain);
            log.debug("Blockchain containing {} blocks successfully saved", blockchain.getSize());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Block generateBlock(Blockchain.Challenge challenge, String miner) throws InvalidBlockException {
        log.debug("Seeking hash for block with {} messages starting with:\n{} (previous block hash: {})",
                challenge.getWorkload().size(), challenge.getSeekingString(), challenge.getLastHash());

        String currentHash = "";
        long salt = 0L;

        while (!currentHash.startsWith(challenge.getSeekingString())) {
            if (challenge.isCompleted()) {
                throw new InvalidBlockException("Generation aborted. Current challenge is completed, asking for new...");
            }
            salt = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
            currentHash = calculateHash(challenge.getWorkload().hashCode() + challenge.getComplexity() + salt + challenge.getLastHash());
        }
        log.debug("Found appropriate hash\n({}) with salt: {}", currentHash, salt);

        return new Block(challenge.getId(), challenge.getComplexity(), salt, currentHash, miner, challenge.getWorkload());
    }

    public static String calculateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b );
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
