package dev.sentinel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sentinel.async.FeedProcessor;
import dev.sentinel.async.FeedStore;
import dev.sentinel.async.InvalidFeedException;
import dev.sentinel.transform.OrderFeedMapper;
import dev.sentinel.validation.SchemaRegistry;
import dev.sentinel.validation.XmlValidationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Plain unit tests: no Spring context and no RabbitMQ broker needed. */
class FeedProcessorTest {

    private FeedStore store;
    private FeedProcessor processor;

    private static final String VALID = """
            <orderFeed><order id="1"><customer>Ada</customer>
            <total currency="CAD">5.00</total><item sku="A" qty="1"/></order></orderFeed>""";

    private static final String INVALID = """
            <orderFeed><order id="x"><customer>Ada</customer>
            <total currency="EUR">5.00</total><item sku="A" qty="1"/></order></orderFeed>""";

    @BeforeEach
    void setUp() {
        store = new FeedStore();
        processor = new FeedProcessor(new XmlValidationService(new SchemaRegistry()),
                new OrderFeedMapper(), store, new SimpleMeterRegistry());
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void validFeedIsMarkedProcessed() {
        processor.process("f1", "v1", bytes(VALID));
        assertEquals(FeedStore.Status.PROCESSED, store.find("f1").orElseThrow().status());
        assertEquals(1, store.find("f1").orElseThrow().orders().size());
    }

    @Test
    void invalidFeedIsRejectedForTheDeadLetterQueue() {
        assertThrows(InvalidFeedException.class, () -> processor.process("f2", "v1", bytes(INVALID)));
        FeedStore.FeedRecord rec = store.find("f2").orElseThrow();
        assertEquals(FeedStore.Status.FAILED, rec.status());
        assertEquals(true, rec.errors().size() > 0);
    }

    @Test
    void unknownSchemaVersionIsRejectedNotLost() {
        assertThrows(InvalidFeedException.class, () -> processor.process("f3", "v9", bytes(VALID)));
        assertEquals(FeedStore.Status.FAILED, store.find("f3").orElseThrow().status());
    }
}
