package com.taskflow.notification.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit-level verification that the consumer handles duplicate messages without
 * throwing.  Full Kafka integration (EmbeddedKafka round-trip) is in
 * NotificationEventConsumerIT, which requires a running broker and is gated
 * behind the same assumeTrue pattern used in the other *IT tests.
 */
@ExtendWith(SpringExtension.class)
class NotificationEventConsumerTest {

    private final NotificationEventConsumer consumer = new NotificationEventConsumer();

    @Test
    void consume_doesNotThrowOnFirstDelivery() {
        String payload = """
                {"eventId":"uuid-1","eventType":"task.created","projectId":1,"taskId":2}
                """;
        assertDoesNotThrow(() -> consumer.consume(payload, "uuid-1"));
    }

    @Test
    void consume_doesNotThrowOnDuplicateDelivery() {
        String payload = """
                {"eventId":"uuid-2","eventType":"task.assigned","projectId":1,"taskId":2}
                """;
        assertDoesNotThrow(() -> {
            consumer.consume(payload, "uuid-2");
            consumer.consume(payload, "uuid-2");
        });
    }
}
