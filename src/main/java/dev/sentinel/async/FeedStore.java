package dev.sentinel.async;

import dev.sentinel.transform.Order;
import dev.sentinel.validation.ValidationError;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** In-memory status tracking. Phase 3 replaces this with PostgreSQL. */
@Component
public class FeedStore {

    public enum Status { QUEUED, PROCESSED, FAILED }

    public record FeedRecord(String feedId, String schemaVersion, Status status,
                             List<Order> orders, List<ValidationError> errors, Instant updatedAt) {}

    private final Map<String, FeedRecord> records = new ConcurrentHashMap<>();

    public void queued(String feedId, String version) {
        records.put(feedId, new FeedRecord(feedId, version, Status.QUEUED, null, null, Instant.now()));
    }

    public void processed(String feedId, String version, List<Order> orders) {
        records.put(feedId, new FeedRecord(feedId, version, Status.PROCESSED, orders, null, Instant.now()));
    }

    public void failed(String feedId, String version, List<ValidationError> errors) {
        records.put(feedId, new FeedRecord(feedId, version, Status.FAILED, null, errors, Instant.now()));
    }

    public Optional<FeedRecord> find(String feedId) {
        return Optional.ofNullable(records.get(feedId));
    }
}
