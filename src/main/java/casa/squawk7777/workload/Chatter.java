package casa.squawk7777.workload;

import casa.squawk7777.SecurityHelper;
import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;

public class Chatter extends RecursiveAction {
    private static final Logger log = LoggerFactory.getLogger(Chatter.class);
    private static final long MESSAGE_DELAY_MIN = 500L;
    private static final long MESSAGE_DELAY_MAX = 2000L;

    private final transient Chat chat;
    private final transient Faker faker;
    private final KeyPair keys;

    private int messagesLeft;

    public Chatter(KeyPair keys, Chat chat, Faker faker, int messageLimit) {
        this.keys = keys;
        this.chat = chat;
        this.faker = faker;
        this.messagesLeft = messageLimit;
    }

    @Override
    protected void compute() {
        do {
            try {
                String randomMessage = String.format("%s: %s", faker.funnyName().name(), faker.twinPeaks().quote());

                Integer messageId = chat.getNextMessageId();
                byte[] signature = SecurityHelper.signData(messageId + randomMessage, keys.getPrivate());

                Message message = new Message(messageId, randomMessage, signature, keys.getPublic());

                chat.pushMessage(message);

                long delay = ThreadLocalRandom.current().nextLong(MESSAGE_DELAY_MIN, MESSAGE_DELAY_MAX + 1);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                log.debug("Thread interrupted: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch (GeneralSecurityException e) {
                log.debug("Unable to sign message: {}", e.getMessage(), e);
            }
        } while (--messagesLeft > 0);
    }
}
