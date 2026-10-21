package com.taskflow.notification.infrastructure;

import com.taskflow.notification.domain.NotificationEvent;
import com.taskflow.notification.domain.OutboxEntry;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JdbcOutboxRepository {

    private final JdbcClient jdbc;

    public JdbcOutboxRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(NotificationEvent event) {
        jdbc.sql("""
                INSERT INTO outbox_events (event_id, event_type, payload)
                VALUES (:eventId::uuid, :eventType, :payload::jsonb)
                ON CONFLICT (event_id) DO NOTHING
                """)
                .param("eventId", event.eventId().toString())
                .param("eventType", event.eventType())
                .param("payload", toJson(event))
                .update();
    }

    public List<OutboxEntry> findUndelivered(int limit) {
        return jdbc.sql("""
                SELECT id, event_id, event_type, payload::text, delivered, attempts, created_at
                FROM outbox_events
                WHERE delivered = FALSE AND attempts < 5
                ORDER BY created_at
                LIMIT :limit
                """)
                .param("limit", limit)
                .query((rs, rowNum) -> new OutboxEntry(
                        rs.getLong("id"),
                        UUID.fromString(rs.getString("event_id")),
                        rs.getString("event_type"),
                        rs.getString("payload"),
                        rs.getBoolean("delivered"),
                        rs.getInt("attempts"),
                        rs.getTimestamp("created_at").toInstant()
                ))
                .list();
    }

    public void markDelivered(long id) {
        jdbc.sql("UPDATE outbox_events SET delivered = TRUE WHERE id = :id")
                .param("id", id)
                .update();
    }

    public void incrementAttempts(long id) {
        jdbc.sql("UPDATE outbox_events SET attempts = attempts + 1 WHERE id = :id")
                .param("id", id)
                .update();
    }

    private static String toJson(NotificationEvent e) {
        return String.format(
                "{\"eventId\":\"%s\",\"eventType\":\"%s\",\"projectId\":%d," +
                "\"taskId\":%d,\"actorId\":%d,\"assigneeId\":%s," +
                "\"previousAssigneeId\":%s,\"occurredAt\":\"%s\"}",
                e.eventId(), e.eventType(), e.projectId(),
                e.taskId(), e.actorId(),
                e.assigneeId() != null ? e.assigneeId() : "null",
                e.previousAssigneeId() != null ? e.previousAssigneeId() : "null",
                e.occurredAt()
        );
    }
}
