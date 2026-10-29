package dev.sentinel.api;

import dev.sentinel.transform.Order;
import dev.sentinel.transform.OrderFeedMapper;
import dev.sentinel.validation.ValidationError;
import dev.sentinel.validation.XmlValidationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{version}/feeds")
public class FeedController {

    private final XmlValidationService validation;
    private final OrderFeedMapper mapper;

    public FeedController(XmlValidationService validation, OrderFeedMapper mapper) {
        this.validation = validation;
        this.mapper = mapper;
    }

    /** Validate only: 200 if clean, 422 with every error otherwise. */
    @PostMapping(value = "/validate", consumes = {"application/xml", "text/xml"})
    public ResponseEntity<Map<String, Object>> validate(@PathVariable String version, @RequestBody byte[] body) {
        List<ValidationError> errors = validation.validate(body, version);
        return errors.isEmpty()
                ? ResponseEntity.ok(Map.of("valid", true))
                : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("valid", false, "errors", errors));
    }

    /** Validate and convert: returns canonical JSON on success, 422 with errors on failure. */
    @PostMapping(consumes = {"application/xml", "text/xml"})
    public ResponseEntity<Object> ingest(@PathVariable String version, @RequestBody byte[] body) {
        List<ValidationError> errors = validation.validate(body, version);
        if (!errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("valid", false, "errors", errors));
        }
        List<Order> orders = mapper.map(body);
        return ResponseEntity.ok(Map.of("valid", true, "schemaVersion", version, "orders", orders));
    }
}
