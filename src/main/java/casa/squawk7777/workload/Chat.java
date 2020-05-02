package casa.squawk7777.workload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Chat implements WorkloadSupplier<Message> {
    private static final Logger log = LoggerFactory.getLogger(Chat.class);

    private final BlockingQueue<Message> messages;
    private final AtomicInteger lastMessageId = new AtomicInteger(0);

    public Chat() {
        this.messages = new LinkedBlockingQueue<>();
    }

    public Integer getNextMessageId() {
        return lastMessageId.incrementAndGet();
    }

    public void pushMessage(Message message) throws InterruptedException {
        log.debug("Pushing new message: {}", message);
        messages.put(message);
    }

    @Override
    public Set<Message> fetchWorkload() {
        Set<Message> messageList = new TreeSet<>();

        log.debug("Fetching {} stored messages", messages.size());
        messages.drainTo(messageList);
        return messageList;
    }
}
