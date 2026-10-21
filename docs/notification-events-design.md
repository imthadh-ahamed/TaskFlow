# Notification Events Design

**Status:** Design only — no messaging infrastructure added (T13.3 scope)  
**Date:** 2026-10-21

## Goals

Decouple slow notification delivery (email, push, in-app) from user-facing task operations so that a notification provider outage never blocks task creation or assignment.

---

## Event Contracts

### `task.created`

Fired when a new task is created inside a project.

```json
{
  "eventType": "task.created",
  "eventId": "<uuid>",
  "occurredAt": "<ISO-8601 UTC>",
  "projectId": 123,
  "taskId": 456,
  "createdBy": 7,
  "assigneeId": 9
}
```

**Rules:**
- `assigneeId` may be `null` (task created without assignment).
- No task title, description, or user PII in the payload — consumers fetch those from the API if needed.

### `task.assigned`

Fired when an existing task's assignee changes (set, changed, or cleared).

```json
{
  "eventType": "task.assigned",
  "eventId": "<uuid>",
  "occurredAt": "<ISO-8601 UTC>",
  "projectId": 123,
  "taskId": 456,
  "assignedBy": 7,
  "previousAssigneeId": 8,
  "newAssigneeId": 9
}
```

**Rules:**
- `previousAssigneeId` is `null` on first assignment.
- `newAssigneeId` is `null` when assignee is cleared.
- A consumer that receives this event must send a notification only to `newAssigneeId` (not the previous one).

---

## What Belongs in an Event

| Include | Exclude |
|---------|---------|
| Resource IDs (`taskId`, `projectId`, `userId`) | Task title / description (could contain sensitive data) |
| Action type and actor | User email, display name, password |
| Timestamp | Session tokens, JWT |
| Correlation ID (`eventId`) for idempotency | Any field not required for routing decisions |

Consumers must call back to the TaskFlow REST API to fetch display details. This keeps event payloads stable and avoids re-publishing events when mutable fields change.

---

## Transactional Outbox Pattern

### Problem

A naive implementation that calls a message broker inside the same request can lose events:
- DB commit succeeds, broker publish fails → silent data loss.
- Broker publish succeeds, DB commit fails → phantom event (notification for a task that doesn't exist).

### Proposed Solution

Add an `outbox_events` table. The application inserts a row into this table **in the same transaction** as the task write. A separate relay process reads undelivered rows and publishes them to the broker.

```sql
-- database/migrations/V9__outbox_events.sql  (not yet added — T13.4 scope)
CREATE TABLE outbox_events (
    id          BIGSERIAL PRIMARY KEY,
    event_type  VARCHAR(64)  NOT NULL,
    event_id    UUID         NOT NULL UNIQUE,
    payload     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    delivered   BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX ON outbox_events (delivered, created_at);
```

The `event_id` UUID is the idempotency key. Consumers that have already processed a given `event_id` should skip it (store processed IDs in a delivered-events table or use broker-level deduplication).

**Relay process (T13.4 scope):** A `@Scheduled` or Kafka producer reads rows where `delivered = false`, publishes them, then marks `delivered = true`. A retry limit (e.g., 5 attempts) and a dead-letter queue prevent infinite loops on persistent failures.

---

## Failure Behaviour

| Failure scenario | Behaviour |
|-----------------|-----------|
| Broker down at publish time | Row stays in `outbox_events` with `delivered = false`; relay retries on next poll |
| Relay crashes mid-flight | Row stays undelivered; relay is idempotent and re-tries on restart |
| Duplicate delivery to consumer | Consumer deduplicates by `event_id` |
| Consumer processing fails | Consumer retries; after N retries, moves event to dead-letter queue |
| Database unavailable | Task write fails (expected); no outbox row written; no event published |

---

## What Is NOT in This Design

- Kafka topic names, partition counts, consumer group IDs → T13.4
- Relay implementation, retry limits, DLQ config → T13.4
- Migration file `V9__outbox_events.sql` → T13.4 (only added once infra is confirmed)
- In-app notification storage or delivery → out of scope for this project phase
