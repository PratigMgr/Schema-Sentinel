package dev.sentinel.async;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FeedListener {

    private final FeedProcessor processor;

    public FeedListener(FeedProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = RabbitConfig.INCOMING)
    public void onMessage(Message message) {
        String feedId = String.valueOf(message.getMessageProperties().getHeaders().get("feedId"));
        String version = String.valueOf(message.getMessageProperties().getHeaders().get("schemaVersion"));
        processor.process(feedId, version, message.getBody());
    }
}
