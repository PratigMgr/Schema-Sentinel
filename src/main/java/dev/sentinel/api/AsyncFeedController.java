package dev.sentinel.api;

import dev.sentinel.async.FeedStore;
import dev.sentinel.async.RabbitConfig;
import dev.sentinel.validation.SchemaRegistry;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsyncFeedController {

    private final RabbitTemplate rabbit;
    private final AmqpAdmin admin;
    private final FeedStore store;
    private final SchemaRegistry schemas;

    public AsyncFeedController(RabbitTemplate rabbit, AmqpAdmin admin, FeedStore store, SchemaRegistry schemas) {
        this.rabbit = rabbit;
        this.admin = admin;
        this.store = store;
        this.schemas = schemas;
    }

    /** Accepts a feed and returns immediately (202). A worker validates it in the background. */
    @PostMapping(value = "/api/{version}/feeds/async", consumes = {"application/xml", "text/xml"})
    public ResponseEntity<Map<String, Object>> submit(@PathVariable String version, @RequestBody byte[] body) {
        schemas.get(version); // unknown version -> 404 before anything is queued
        String feedId = UUID.randomUUID().toString();
        store.queued(feedId, version);
        rabbit.send("", RabbitConfig.INCOMING, message(feedId, version, body));
        return ResponseEntity.accepted().body(Map.of(
                "feedId", feedId,
                "status", "QUEUED",
                "statusUrl", "/api/feeds/" + feedId));
    }

    @GetMapping("/api/feeds/{feedId}")
    public ResponseEntity<FeedStore.FeedRecord> status(@PathVariable String feedId) {
        return store.find(feedId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Moves everything in the dead-letter queue back to the main queue.
     * Optional ?version=v2 re-processes the feeds against a different schema version.
     * Only the messages present when the call starts are replayed, so a feed that fails again
     * cannot cause an endless loop.
     */
    @PostMapping("/api/admin/dlq/replay")
    public Map<String, Object> replay(@RequestParam(required = false) String version) {
        if (version != null) {
            schemas.get(version);
        }
        QueueInformation info = admin.getQueueInfo(RabbitConfig.DEAD);
        int pending = info == null ? 0 : info.getMessageCount();
        int replayed = 0;
        for (int i = 0; i < pending; i++) {
            Message dead = rabbit.receive(RabbitConfig.DEAD, 500);
            if (dead == null) {
                break;
            }
            Map<String, Object> headers = dead.getMessageProperties().getHeaders();
            String feedId = String.valueOf(headers.get("feedId"));
            String useVersion = version != null ? version : String.valueOf(headers.get("schemaVersion"));
            store.queued(feedId, useVersion);
            rabbit.send("", RabbitConfig.INCOMING, message(feedId, useVersion, dead.getBody()));
            replayed++;
        }
        return Map.of("replayed", replayed);
    }

    private static Message message(String feedId, String version, byte[] body) {
        return MessageBuilder.withBody(body)
                .setContentType("application/xml")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("feedId", feedId)
                .setHeader("schemaVersion", version)
                .build();
    }
}

