package casa.squawk7777;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Chat {
    private static final Logger log = LoggerFactory.getLogger(Chat.class);
    private BlockingQueue<String> messages;

    public Chat() {
        this.messages = new LinkedBlockingQueue<>();
    }

    public Collection<String> fetchMessages() {
        List<String> messageList = new ArrayList<>();

        log.debug("Fetching {} stored messages", messages.size());
        messages.drainTo(messageList);
        return messageList;
    }

    public void pushMessage(String message) throws InterruptedException {
        log.debug("Pushing new message: {}", message);
        messages.put(message);
    }
}
