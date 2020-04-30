package casa.squawk7777;

import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;

public class Chatter extends RecursiveAction {
    private static final Logger log = LoggerFactory.getLogger(Chatter.class);
    private static final Long MESSAGE_DELAY_MIN = 500L;
    private static final Long MESSAGE_DELAY_MAX = 5000L;

    private transient Chat chat;
    private transient Faker faker;
    private Integer messageLimit;

    public Chatter(Chat chat, Faker faker, Integer messageLimit) {
        this.chat = chat;
        this.faker = faker;
        this.messageLimit = messageLimit;
    }

    @Override
    protected void compute() {
        do {
            String randomMessage = String.format("%s: %s", faker.funnyName().name(), faker.twinPeaks().quote());
            log.debug("Generated message: {}", randomMessage);

            try {
                chat.pushMessage(randomMessage);

                long delay = ThreadLocalRandom.current().nextLong(MESSAGE_DELAY_MIN, MESSAGE_DELAY_MAX + 1);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                log.debug("Thread interrupted: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        } while (--messageLimit > 0);
    }
}
