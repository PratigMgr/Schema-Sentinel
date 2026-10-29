package dev.sentinel.async;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/** Permanent failure: reject without requeue so RabbitMQ routes the message to the dead-letter queue. */
public class InvalidFeedException extends AmqpRejectAndDontRequeueException {
    public InvalidFeedException(String message) {
        super(message);
    }
}
