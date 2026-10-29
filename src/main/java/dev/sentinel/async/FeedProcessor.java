package dev.sentinel.async;

import dev.sentinel.transform.Order;
import dev.sentinel.transform.OrderFeedMapper;
import dev.sentinel.validation.UnknownSchemaVersionException;
import dev.sentinel.validation.ValidationError;
import dev.sentinel.validation.XmlValidationService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.stereotype.Service;

/** Business logic for one feed, kept separate from RabbitMQ so it can be unit-tested without a broker. */
@Service
public class FeedProcessor {

    private final XmlValidationService validation;
    private final OrderFeedMapper mapper;
    private final FeedStore store;
    private final MeterRegistry metrics;

    public FeedProcessor(XmlValidationService validation, OrderFeedMapper mapper,
                         FeedStore store, MeterRegistry metrics) {
        this.validation = validation;
        this.mapper = mapper;
        this.store = store;
        this.metrics = metrics;
    }

    public void process(String feedId, String version, byte[] xml) {
        List<ValidationError> errors;
        try {
            errors = validation.validate(xml, version);
        } catch (UnknownSchemaVersionException e) {
            errors = List.of(new ValidationError(0, 0, "FATAL", e.getMessage()));
        }
        if (!errors.isEmpty()) {
            store.failed(feedId, version, errors);
            metrics.counter("sentinel.feeds.failed").increment();
            throw new InvalidFeedException("Feed " + feedId + " failed validation (" + errors.size() + " errors)");
        }
        List<Order> orders = mapper.map(xml);
        store.processed(feedId, version, orders);
        metrics.counter("sentinel.feeds.processed").increment();
    }
}
