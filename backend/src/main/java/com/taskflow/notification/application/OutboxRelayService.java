package com.taskflow.notification.application;

import com.taskflow.notification.domain.OutboxEntry;
import com.taskflow.notification.infrastructure.JdbcOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Polls the outbox_events table and forwards undelivered rows to Kafka.
 *
 * KafkaTemplate is optional so the relay bean can be created even when
 * KafkaAutoConfiguration is not active (e.g. integration tests with no
 * running broker). When Kafka is absent the relay logs a warning and skips;
 * outbox rows stay undelivered and will be retried on the next poll.
 *
 * Retry behaviour: rows with attempts >= 5 are skipped (dead-lettered in-place).
 */
@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);
    private static final String TOPIC = "taskflow.notifications";
    private static final int BATCH_SIZE = 50;

    private final JdbcOutboxRepository outboxRepository;
    @SuppressWarnings("rawtypes")
    private final Optional<KafkaTemplate> kafkaTemplate;

    @Autowired
    @SuppressWarnings("rawtypes")
    public OutboxRelayService(JdbcOutboxRepository outboxRepository,
                               Optional<KafkaTemplate> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${taskflow.outbox.relay-interval-ms:5000}")
    @SuppressWarnings("unchecked")
    public void relay() {
        if (kafkaTemplate.isEmpty()) {
            log.debug("outbox: Kafka unavailable, skipping relay");
            return;
        }
        List<OutboxEntry> pending = outboxRepository.findUndelivered(BATCH_SIZE);
        KafkaTemplate template = kafkaTemplate.get();
        for (OutboxEntry entry : pending) {
            try {
                template.send(TOPIC, entry.eventId().toString(), entry.payload()).get();
                outboxRepository.markDelivered(entry.id());
                log.debug("outbox: published {} eventId={}", entry.eventType(), entry.eventId());
            } catch (Exception ex) {
                outboxRepository.incrementAttempts(entry.id());
                log.warn("outbox: failed to publish eventId={} attempt={} error={}",
                        entry.eventId(), entry.attempts() + 1, ex.getMessage());
            }
        }
    }
}
