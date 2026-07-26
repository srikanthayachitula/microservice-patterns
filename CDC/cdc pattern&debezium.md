# Change Data Capture (CDC) & Debezium

## Problem

Suppose an Order Service creates an order.

```
                Order Service
                      │
                      ▼
                Insert Order
                      │
                      ▼
                 PostgreSQL
```

Now multiple systems need to react.

- Inventory
- Notification
- Analytics
- Search
- Billing

Without CDC, the Order Service has to notify every service.

```
             Order Service
                  │
     ┌────────────┼─────────────┐
     ▼            ▼             ▼
 Inventory   Notification   Analytics
                               │
                               ▼
                           Elasticsearch
```

Problems

- Tight coupling
- Hard to add new consumers
- More failure points
- Difficult to maintain

---

# What is Change Data Capture (CDC)?

CDC captures every committed database change and converts it into an event.

Instead of the application telling everyone,

the **database becomes the source of truth**.

```
Application

      │

      ▼

 Database

      │

 INSERT / UPDATE / DELETE

      │

      ▼

 Database Change Event

      │

      ▼

 Interested Services
```

---

# Two Ways to Implement CDC

## 1. Polling

A background worker periodically queries the database.

```
Worker

   │

Every 1 second

   │

   ▼

SELECT * FROM orders
WHERE processed = false

   │

   ▼

Publish Events
```

### Advantages

- Easy to implement
- No extra infrastructure

### Disadvantages

- Constant database queries
- Higher latency
- Doesn't scale well
- Wastes resources

---

## 2. Log-Based CDC (Preferred)

Every database already records committed transactions.

Examples

| Database | Transaction Log |
|----------|-----------------|
| PostgreSQL | WAL |
| MySQL | Binlog |
| SQL Server | Transaction Log |
| Oracle | Redo Log |

Instead of polling tables,

CDC tools simply read the transaction log.

```
Application

      │

      ▼

 Database

      │

 Commit Transaction

      │

      ▼

Transaction Log (WAL)

      │

      ▼

 CDC Tool

      │

      ▼

 Event Stream
```

---

# What is WAL?

WAL = **Write Ahead Log**

Every committed database transaction is first written to the WAL.

```
INSERT Order

      │

      ▼

Commit

      │

      ▼

Write to WAL

      │

      ▼

Persist Data
```

The WAL contains every change made to the database.

CDC tools simply read this log.

---

# What is Debezium?

Debezium is an open-source CDC platform.

It continuously monitors the database transaction log and publishes changes to Kafka.

```
          Application
                │
                ▼
          PostgreSQL
                │
                ▼
         Write Ahead Log
                │
                ▼
            Debezium
                │
                ▼
              Kafka
                │
      ┌─────────┼─────────┐
      ▼         ▼         ▼
 Inventory   Search   Notification
```

---

# Why is Debezium Better than Polling?

Imagine

```
Orders Table

100 Million Rows

↓

Only One Row Changed
```

Polling

```
Worker

↓

Scan Table

↓

Find One Row
```

Very inefficient.

Debezium

```
Database Commit

↓

WAL contains exactly ONE change

↓

Debezium reads only that change

↓

Publish Event
```

No table scan.

Much lower latency.

---

# What Does Debezium Publish?

Database

```sql
INSERT INTO orders(id,status)
VALUES (101,'CREATED');
```

Produces

```json
{
  "before": null,
  "after": {
    "id": 101,
    "status": "CREATED"
  },
  "op": "c"
}
```

Update

```json
{
  "before": {
    "status": "CREATED"
  },
  "after": {
    "status": "PAID"
  },
  "op": "u"
}
```

---

# Why Isn't CDC Alone Enough?

Consider this application flow.

```
Save Order to DB

      │

      ▼

Database Commit ✓

      │

      ▼

Publish Kafka Event ✗

      │

      ▼

Application Crashes
```

Result

```
Database
─────────
Order Exists

Kafka
─────────
No Event
```

The order exists forever.

Other services never know.

---

# Transactional Outbox + Debezium

This is the production architecture used by many companies.

```
               Application
                    │
                    ▼
        ┌──────────────────────┐
        │ Database Transaction │
        ├──────────────────────┤
        │ Orders Table         │
        │ Outbox Table         │
        └──────────────────────┘
                    │
                    ▼
                 Commit
                    │
                    ▼
                   WAL
                    │
                    ▼
                Debezium
                    │
                    ▼
                  Kafka
                    │
        ┌───────────┼────────────┐
        ▼           ▼            ▼
   Inventory    Notification   Search
```

### Responsibilities

**Transactional Outbox**

- Guarantees business data and event metadata are committed atomically.

**Debezium**

- Reliably publishes committed outbox events.

---

# Can Debezium Replace the Outbox Pattern?

Usually **No**.

Suppose Debezium watches the Orders table directly.

```
INSERT Order
```

Produces

```
OrderCreated
```

Good.

Later

```
UPDATE lastViewedTime
```

Also produces an event.

But nobody cares about `lastViewedTime`.

The Outbox allows the application to publish only meaningful business events.

Examples

- OrderCreated
- PaymentSucceeded
- OrderCancelled

instead of every database modification.

---

# Polling vs Debezium

| Polling | Debezium |
|----------|-----------|
| Simple | More complex |
| Polls database repeatedly | Reads transaction log |
| Higher DB load | Minimal DB load |
| Higher latency | Near real-time |
| Easy to build | More operational overhead |
| Good for small systems | Better for high-scale systems |

---

# Failure Scenarios

## Debezium crashes

```
Database

↓

WAL

↓

Debezium ✗
```

No data is lost.

The WAL still contains all committed transactions.

When Debezium restarts

```
Read Last Offset

↓

Continue Reading WAL

↓

Publish Remaining Events
```

---

## Kafka is down

```
Database

↓

WAL

↓

Debezium

↓

Kafka ✗
```

Debezium pauses.

Database transactions continue.

Once Kafka is available,

Debezium resumes publishing.

---

## WAL is deleted

```
Database

↓

Old WAL Removed

↓

Debezium Restarts

↓

Cannot Continue
```

Recovery usually requires taking a fresh snapshot.

This is why WAL retention must be monitored.

---

# Advantages

- Near real-time
- Very low database overhead
- Reliable recovery
- No polling
- Excellent Kafka integration

---

# Disadvantages

- Additional infrastructure
- More operational complexity
- Database-specific setup
- WAL retention must be monitored

---

# Real-world Use Cases

- Event-driven microservices
- Elasticsearch indexing
- Analytics pipelines
- Data warehouse synchronization
- Audit logging
- Cache synchronization

---

# Key Takeaways

```
Problem

↓

Need reliable event publication

↓

Transactional Outbox
(Atomic DB + Event)

↓

Database Commit

↓

WAL

↓

Debezium

↓

Kafka

↓

Consumers
```

**Outbox** solves **atomicity**.


---

# Debezium Offsets

Debezium keeps track of how much of the transaction log it has already processed.

Example:

```
Write Ahead Log (WAL)

L1
L2
L3
L4
L5
```

Suppose Debezium has already published events up to `L3`.

It stores an offset.

```
Last Processed Offset = L3
```

If Debezium crashes:

```
Restart

↓

Read Stored Offset

↓

Resume from L4
```

Without offsets, Debezium would need to reread the entire transaction log.

---

# Initial Snapshot

Consider an existing Orders table with one million rows.

When Debezium is first installed, those existing records are not present in Kafka.

Debezium first performs an initial snapshot.

```
Orders Table

↓

Snapshot

↓

Kafka
```

Once the snapshot completes, Debezium switches to streaming new changes from the transaction log.

```
New Transactions

↓

Write Ahead Log (WAL)

↓

Debezium

↓

Kafka
```

Debezium therefore operates in two phases.

```
Snapshot

↓

Streaming
```

---

# Event Ordering

The transaction log is sequential.

```
Transaction 1

↓

Transaction 2

↓

Transaction 3
```

Debezium reads the transaction log in order and publishes events in the same order for a given database transaction stream.

This preserves the order of committed changes.

---

# Why Use Kafka?

Debezium can publish database changes to Kafka instead of directly calling downstream services.

```
Producer

↓

Kafka

↓

Inventory

Notification

Analytics

Search
```

Benefits:

- Decouples producers and consumers
- Supports multiple independent consumers
- Allows event replay
- Buffers spikes in traffic
- Consumers can scale independently

---

# Committed Transactions Only

Debezium only captures committed transactions.

```
Application

↓

Database Transaction

↓

Commit

↓

Write Ahead Log

↓

Debezium
```

If a transaction rolls back, no event is produced.

Only committed changes appear in Kafka.

---

# CDC vs Event Sourcing

These are different patterns.

### Change Data Capture (CDC)

```
Database

↓

Capture Changes

↓

Events
```

Database changes generate events.

---

### Event Sourcing

```
Events

↓

Replay Events

↓

Database State
```

Events are the source of truth.

Database state is reconstructed from the event history.

---

# Typical Outbox Table Schema

A typical Outbox table contains:

| Column | Description |
|---------|-------------|
| id | Unique Event ID |
| aggregate_type | Business entity (Order, Payment, User) |
| aggregate_id | Entity Identifier |
| event_type | OrderCreated, PaymentSucceeded, etc. |
| payload | Event payload (JSON) |
| created_at | Event creation timestamp |
| published | Whether the event has been published |

This allows:

- Reliable retries
- Event replay
- Filtering
- Auditing

---

# Why Not Monitor Business Tables Directly?

Suppose Debezium monitors the Orders table.

```
Orders

id
status
lastViewedTime
updatedBy
```

Updating `lastViewedTime` produces a CDC event.

```
UPDATE lastViewedTime

↓

CDC Event

↓

Kafka
```

However, downstream services usually do not care about this update.

Using the Outbox pattern allows the application to publish only meaningful business events.

Examples:

- OrderCreated
- OrderCancelled
- PaymentSucceeded

instead of every database modification.

---

# Exactly-Once Delivery

Debezium provides reliable event publication but duplicate events are still possible in distributed systems.

Applications should always assume **at-least-once delivery**.

Consumers should therefore be **idempotent**.

Duplicate events should not cause duplicate business operations.

---

# Production Architecture

```
                  Client
                     │
                     ▼
               Order Service
                     │
         ┌───────────┴───────────┐
         │   Single DB Transaction│
         │------------------------│
         │ Orders Table           │
         │ Outbox Table           │
         └───────────┬────────────┘
                     │
                  Commit
                     │
                     ▼
            PostgreSQL WAL
                     │
                     ▼
                 Debezium
                     │
                     ▼
                   Kafka
        ┌────────────┼────────────┐
        ▼            ▼            ▼
   Inventory   Notification   Search
```

---

# When to Use CDC

Use CDC when:

- Multiple downstream services need database change events.
- Building event-driven microservices.
- Synchronising search indexes.
- Feeding analytics pipelines.
- Synchronising caches.
- Replicating data into data warehouses.
- Reliable event publication is required.

---

# When Not to Use CDC

CDC may not be necessary when:

- Only one application uses the database.
- No downstream consumers exist.
- A simple polling solution is sufficient.
- Access to the transaction log (WAL/Binlog) is unavailable.
- The operational complexity outweighs the benefits.

**Debezium** solves **reliable publication**.

They are complementary patterns and are commonly used together.
