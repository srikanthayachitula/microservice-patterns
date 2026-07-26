# Idempotency

## What is Idempotency?

An operation is **idempotent** if performing it multiple times produces the **same result** as performing it once.

Example:

```
Charge Payment

↓

Success
```

If the same request is received again,

```
Charge Payment

↓

Already Processed

↓

Return Previous Result
```

The customer is **not charged twice**.

---

# Why Do We Need Idempotency?

Distributed systems are unreliable.

Failures can occur at any point.

```
Client

↓

Send Request

↓

Server Processes Request

↓

Response Lost
```

The client never receives the response.

It retries.

```
Client

↓

Retry Request

↓

Server Receives Same Request Again
```

Without idempotency:

```
Payment Charged Twice ❌
```

With idempotency:

```
Duplicate Request

↓

Already Processed

↓

Return Existing Result ✅
```

---

# Where Are Duplicate Requests Created?

Duplicates can occur because of:

- Client retries
- Network timeouts
- Load balancer retries
- Kafka retries
- Consumer crashes
- Service restarts
- Manual retries

Never assume a request will only arrive once.

---

# Example: Payment API

Without idempotency

```
Client

↓

POST /payments

↓

Charge £100

↓

Timeout
```

Client retries.

```
POST /payments

↓

Charge £100 Again ❌
```

Customer pays £200.

---

With idempotency

```
POST /payments

Idempotency-Key: abc123

↓

Charge £100

↓

Store Result
```

Retry

```
POST /payments

Idempotency-Key: abc123

↓

Already Processed

↓

Return Previous Result
```

Customer still pays only £100.

---

# API Idempotency

Clients generate a unique Idempotency Key.

Example

```
POST /payments

Headers

Idempotency-Key: abc123
```

Server Flow

```
Receive Request

↓

Lookup Idempotency Key

↓

Exists?
```

If No

```
Process Request

↓

Store Response

↓

Return Response
```

If Yes

```
Return Stored Response

↓

Do NOT Process Again
```

---

# Database Design

Typical table

| Column | Description |
|---------|-------------|
| idempotency_key | Unique Request Identifier |
| request_hash | Request Payload Hash |
| response | Stored Response |
| status | Success / Failed |
| created_at | Timestamp |

---

# Consumer Idempotency

Consumers can also receive duplicate events.

Example

```
Kafka

↓

OrderCreated

↓

Inventory Service
```

Consumer crashes after updating inventory.

Kafka retries.

```
OrderCreated

↓

Inventory Updated Again ❌
```

Instead

Consumer keeps track of processed events.

```
Processed Events

event_101

event_102

event_103
```

Flow

```
Receive Event

↓

Already Processed?
```

No

```
Process Event

↓

Save Event ID
```

Yes

```
Ignore Duplicate
```

---

# Common Implementations

## 1. Unique Constraint

```
PaymentID

UNIQUE
```

Duplicate insert fails automatically.

---

## 2. Idempotency Key Table

Store processed requests.

Most common for REST APIs.

---

## 3. Processed Event Table

Store processed event IDs.

Common for Kafka consumers.

---

## 4. Business State Check

Example

```
Order Already SHIPPED

↓

Ignore Duplicate Shipment
```

Useful when the business state itself prevents duplicate processing.

---

# Idempotency vs Deduplication

Deduplication

```
Remove Duplicate Messages
```

Idempotency

```
Allow Duplicates

↓

Safely Ignore Them
```

Distributed systems should assume duplicates can happen.

Applications should be idempotent.

---

# Failure Scenarios

## Client Timeout

```
Client

↓

Request

↓

Server Success

↓

Response Lost

↓

Retry
```

Server returns previous result.

---

## Consumer Crash

```
Receive Event

↓

Update Database

↓

Crash Before ACK
```

Broker retries.

Consumer checks processed events before handling again.

---

## Kafka Duplicate Delivery

```
Event

↓

Consumer

↓

Crash

↓

Kafka Retry

↓

Same Event Again
```

Event ID prevents duplicate processing.

---

# Advantages

- Safe retries
- Prevents duplicate business operations
- Improves reliability
- Required for distributed systems
- Simplifies recovery after failures

---

# Disadvantages

- Additional storage
- More application logic
- Idempotency records require cleanup
- Unique keys must be managed carefully

---

# Cleaning Up Idempotency Records

Processed records cannot be kept forever.

Common strategies:

- Delete after retention period
- Archive old records
- Partition by date
- Expire using TTL

Retention depends on business requirements.

---

# Production Architecture

```
              Client
                 │
                 ▼
          Payment Service
                 │
                 ▼
      Lookup Idempotency Key
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
    Key Exists?         No Key
        │                 │
        ▼                 ▼
Return Stored       Process Payment
Response                 │
                          ▼
                 Store Result + Key
                          │
                          ▼
                   Return Response
```

---

# When to Use Idempotency

Use idempotency when:

- Payment processing
- Order creation
- Inventory updates
- Kafka consumers
- REST APIs with retries
- Message queues
- External API calls
- Financial transactions

---

# When Not to Use Idempotency

Idempotency is generally unnecessary for:

- Read-only operations
- Internal computations without side effects
- Temporary cache lookups
- Operations where duplicate execution has no impact

---

# Best Practices

- Always assume requests may be retried.
- Every business operation should have a unique identifier.
- Store processed request or event IDs.
- Consumers should always be idempotent.
- Never rely on exactly-once delivery.
- Use idempotency together with retries and transactional outbox.

---

# Key Takeaways

```
Problem

↓

Duplicate Requests

↓

Duplicate Processing

↓

Idempotency

↓

Same Request

↓

Same Result

↓

No Duplicate Business Operation
```

Idempotency allows systems to safely retry operations without causing duplicate side effects.

It is one of the fundamental building blocks of reliable distributed systems.
