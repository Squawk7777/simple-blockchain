package casa.squawk7777;

import casa.squawk7777.exceptions.InvalidBlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ThreadLocalRandom;

public class BlockchainUtil {
    private static final Logger log = LoggerFactory.getLogger(BlockchainUtil.class);
    private BlockchainUtil() {}

    public static Block generateBlock(Blockchain.Challenge challenge, String miner) throws InvalidBlockException {
        log.debug("Seeking hash for block with {} messages starting with:\n{} (previous block hash: {})",
                challenge.getTransactions().size(), challenge.getSeekingString(), challenge.getLastHash());

        String currentHash = "";
        long salt = 0L;

        while (!currentHash.startsWith(challenge.getSeekingString())) {
            if (challenge.isDone()) {
                throw new InvalidBlockException("Generation aborted. Current challenge is completed, asking for new...");
            }
            salt = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
            currentHash = calculateHash(challenge.getTransactions().hashCode() + challenge.getComplexity() + salt + challenge.getLastHash());
        }
        log.debug("Found appropriate hash\n({}) with salt: {}", currentHash, salt);

        return new Block(challenge.getNextBlockId(), challenge.getComplexity(), salt, currentHash, miner, challenge.getTransactions());
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
