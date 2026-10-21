package com.taskflow.notification.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

/**
 * Listens on the notifications topic and processes each event exactly once.
 *
 * Idempotency: Kafka's enable.idempotence=true and the outbox event_id UUID
 * provide two layers of deduplication. This consumer logs the event; a real
 * implementation would persist a "delivered" record keyed by event_id and
 * skip re-processing if the key already exists.
 *
 * Dead-letter handling: Spring Kafka's DefaultErrorHandler (configured in
 * KafkaConfig) retries up to 3 times with exponential backoff, then forwards
 * the message to the taskflow.notifications.DLT topic.
 */
@Service
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    @KafkaListener(
            topics = "taskflow.notifications",
            groupId = "${spring.kafka.consumer.group-id:taskflow-notifications}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String eventId
    ) {
        log.info("notification: received eventId={} payload={}", eventId, payload);
    }
}
