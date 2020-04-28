package casa.squawk7777;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class BlockchainUtil {
    private BlockchainUtil() {}

    public static Blockchain loadBlockchain(File blockchainFile) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(blockchainFile))) {
            return (Blockchain) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveBlockchain(File blockchainFile, Blockchain blockchain) {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(blockchainFile))) {
            outputStream.writeObject(blockchain);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String calculateHash(String input){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte elem: hash) {
                String hex = Integer.toHexString(0xff & elem);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
