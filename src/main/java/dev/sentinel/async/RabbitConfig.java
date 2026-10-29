package dev.sentinel.async;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology: feeds.incoming --(rejected)--> feeds.dlx --> feeds.dead
 * Anything the worker rejects is dead-lettered instead of lost or retried forever.
 */
@Configuration
public class RabbitConfig {

    public static final String INCOMING = "feeds.incoming";
    public static final String DEAD = "feeds.dead";
    public static final String DLX = "feeds.dlx";

    @Bean
    Queue incomingQueue() {
        return QueueBuilder.durable(INCOMING)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DEAD)
                .build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    @Bean
    Queue deadQueue() {
        return QueueBuilder.durable(DEAD).build();
    }

    @Bean
    Binding deadBinding() {
        return BindingBuilder.bind(deadQueue()).to(deadLetterExchange()).with(DEAD);
    }
}
