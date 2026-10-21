package com.taskflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.ExponentialBackOff;
import java.util.Optional;

/**
 * Kafka topic declarations and error-handling configuration.
 *
 * Retry policy: up to 3 attempts with exponential back-off starting at 1 s,
 * max 10 s.  After 3 failed attempts the message is forwarded to the DLT
 * (Dead-Letter Topic) named taskflow.notifications.DLT.
 */
@Configuration
@EnableScheduling
public class KafkaConfig {

    public static final String NOTIFICATIONS_TOPIC = "taskflow.notifications";
    public static final String NOTIFICATIONS_DLT   = NOTIFICATIONS_TOPIC + ".DLT";

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(NOTIFICATIONS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationsDlt() {
        return TopicBuilder.name(NOTIFICATIONS_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    @SuppressWarnings("rawtypes")
    public DefaultErrorHandler errorHandler(Optional<KafkaTemplate> kafkaTemplate) {
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxInterval(10_000L);
        backOff.setMaxAttempts(3);

        if (kafkaTemplate.isEmpty()) {
            return new DefaultErrorHandler(backOff);
        }

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate.get(),
                (record, ex) -> new TopicPartition(NOTIFICATIONS_DLT, 0));

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
