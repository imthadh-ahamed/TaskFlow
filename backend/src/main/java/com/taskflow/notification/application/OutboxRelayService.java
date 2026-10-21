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

/**
 * Polls the outbox_events table and forwards undelivered rows to Kafka.
 *
 * KafkaTemplate is injected with required=false so the relay bean can be
 * created even when no Kafka broker is available. When kafkaTemplate is null
 * the relay skips and outbox rows accumulate until Kafka becomes reachable.
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
    private KafkaTemplate kafkaTemplate;

    public OutboxRelayService(JdbcOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Autowired(required = false)
    @SuppressWarnings("rawtypes")
    void setKafkaTemplate(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${taskflow.outbox.relay-interval-ms:5000}")
    @SuppressWarnings("unchecked")
    public void relay() {
        if (kafkaTemplate == null) {
            log.debug("outbox: Kafka unavailable, skipping relay");
            return;
        }
        List<OutboxEntry> pending = outboxRepository.findUndelivered(BATCH_SIZE);
        KafkaTemplate<String, String> template = kafkaTemplate;
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
