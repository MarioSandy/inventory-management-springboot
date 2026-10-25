# Inventory Management System

A Spring Boot REST API for managing a fashion inventory and order lifecycle. The project focuses on the backend problems that arise when product variants, inventory quantities, customer balances, bulk imports, and asynchronous order processing must remain consistent.

## Overview

The system supports three primary roles:

- **USER_ADMIN** — manages user access, including registering customers and warehouse staff.
- **CUSTOMER** — browses the catalog and places orders.
- **WAREHOUSE_STAFF** — handles inventory operations and warehouse workflows, including stock opname review.

Core capabilities include product and variant management, inventory tracking, customer balance reservation, order processing, audit logs, bulk Excel imports, and stock opname with discrepancy review.

## Problems this project addresses

- **Inventory consistency:** prevent overselling and keep physical stock, reserved stock, and order state aligned.
- **Concurrent operations:** coordinate order processing with stock opname and other inventory changes.
- **Reliable background work:** use a transactional outbox to persist work in PostgreSQL before dispatching events to RabbitMQ.
- **Duplicate delivery:** design asynchronous processing so retries and repeated messages do not cause duplicate business effects.
- **Bulk data processing:** process large Excel uploads in chunks and record row-level errors without losing import progress.
- **Financial auditability:** track balance changes, reserved balances, and order-related balance movements.
- **Stock discrepancy handling:** freeze affected inventory during stock opname and route discrepancies that require human review through an explicit resolution workflow.

## Technology stack

- **Language:** Java 17
- **Framework:** Spring Boot 4.1.1
- **Persistence:** Spring Data JPA, Hibernate, PostgreSQL
- **Database migrations:** Flyway
- **Messaging:** RabbitMQ, Spring AMQP
- **Security:** Spring Security and OAuth2 Authorization Server dependencies
- **File storage:** Amazon S3
- **Excel processing:** Apache POI and Excel Streaming Reader
- **Build:** Maven
- **Utilities:** Lombok

See [pom.xml](pom.xml) for the dependency configuration.

## Architecture

~~~mermaid
flowchart TB
    Client[Client] --> API[Spring Boot REST API]
    API --> Domain[Domain services]
    Domain --> DB[(PostgreSQL)]
    Domain --> S3[(Amazon S3)]
    Scheduler[Scheduled jobs] --> DB
    DB --> Outbox[Transactional outbox]
    Outbox --> Dispatcher[Outbox dispatcher]
    Dispatcher --> MQ[RabbitMQ]
    MQ --> Consumer[Message consumers]
    Consumer --> DB
~~~

PostgreSQL stores the source-of-truth business state. The outbox bridges database transactions and asynchronous messaging: business changes and the corresponding outbox records are committed together, then a dispatcher publishes the events to RabbitMQ. Consumers must be safe under retries and duplicate delivery.

S3 stores uploaded spreadsheet files; database records track import progress and row-level failures.

## Main business flows

### 1. Product catalog and inventory

1. An administrator maintains product master data: category, brand, product, color, and size.
2. A product can have multiple variants. A variant represents the sellable combination and has its own SKU and price.
3. Inventory is tracked at the product-variant level, including available and reserved quantities.
4. Inventory mutations are recorded in inventory logs for traceability.

### 2. Order processing

~~~mermaid
sequenceDiagram
    actor Customer
    participant API as Spring Boot API
    participant DB as PostgreSQL
    participant Outbox as Outbox dispatcher
    participant MQ as RabbitMQ
    participant Worker as Order consumer

    Customer->>API: Create order
    API->>DB: Validate request and persist order
    API->>DB: Reserve balance / persist audit records
    API->>DB: Insert outbox event
    Note over API,DB: Commit business state and outbox atomically
    Outbox->>DB: Claim pending outbox rows
    Outbox->>MQ: Publish order event
    MQ->>Worker: Deliver message
    Worker->>DB: Validate and reserve inventory
    Worker->>DB: Update order state and audit records
~~~

The asynchronous order workflow separates request acceptance from inventory reservation and later fulfillment. Inventory availability and freeze state must be checked during processing, not trusted from an earlier catalog response. Order state transitions, balance reservations, inventory reservations, cancellation, and refunds must be coordinated transactionally and remain idempotent.

### 3. Stock opname

~~~mermaid
flowchart TD
    Upload[Upload stock opname spreadsheet] --> Store[Store file and import metadata]
    Store --> Event[Create outbox event]
    Event --> Queue[Dispatch to RabbitMQ]
    Queue --> Parse[Parse spreadsheet in chunks]
    Parse --> Freeze[Freeze affected inventory]
    Freeze --> Compare{Compare actual and system quantity}
    Compare -->|Matches| Finish[Complete item]
    Compare -->|Actual is higher| Reconcile[Reconcile inventory]
    Compare -->|Actual is lower| Review[Create review record]
    Review --> Decision{Reviewer resolution}
    Decision -->|Reconcile to actual| Reconcile
    Decision -->|Reject / recount| Recount[Keep discrepancy for follow-up]
    Decision -->|Reservation shortage| Shortage[Resolve affected reservations]
    Reconcile --> Unfreeze[Complete resolution and release freeze]
    Shortage --> Unfreeze
~~~

Stock opname is treated as a workflow rather than a simple quantity update. The upload is processed asynchronously, affected variants are protected from conflicting inventory operations, and discrepancies can be reviewed before the inventory is released back to normal operations.

### 4. Bulk product import

1. Upload an Excel file and store it in S3.
2. Persist import metadata and enqueue processing through the outbox.
3. Process the file in bounded chunks to control memory usage.
4. Track chunk progress and persist row-level validation or processing errors.
5. Expose import status so the client can distinguish pending work from completed work with errors.

## Data model

The main domain entities and relationships are shown below. The diagram is a conceptual overview; consult the Flyway migrations for the exact PostgreSQL DDL and constraints.

~~~mermaid
erDiagram
    USER ||--o{ USER_SESSION : has
    USER ||--o{ ORDER : places
    USER ||--o| USER_BALANCE : owns
    USER ||--o{ USER_BALANCE_LOG : records
    USER ||--o{ DATA_BULK_IMPORT_LOG : initiates
    PRODUCT_CATEGORY ||--o{ PRODUCT : classifies
    PRODUCT_BRAND ||--o{ PRODUCT : brands
    PRODUCT ||--o{ PRODUCT_VARIANT : has
    PRODUCT_COLOR ||--o{ PRODUCT_VARIANT : defines
    PRODUCT_SIZE ||--o{ PRODUCT_VARIANT : defines
    PRODUCT_VARIANT ||--o| INVENTORY : tracks
    PRODUCT_VARIANT ||--o{ INVENTORY_LOG : audits
    ORDER ||--|{ ORDER_DETAIL : contains
    PRODUCT_VARIANT ||--o{ ORDER_DETAIL : ordered_as
    PRODUCT_VARIANT ||--o{ STOCK_OPNAME_REVIEW : reviewed
    DATA_BULK_IMPORT_LOG ||--o{ DATA_BULK_IMPORT_LOG_ERROR : contains
    USER ||--o{ OUTBOX : creates
~~~

### Main domain areas

| Domain | Responsibilities |
|---|---|
| Users and sessions | User identity, roles, session metadata |
| Product catalog | Categories, brands, products, colors, sizes, variants, SKU and price |
| Inventory | Variant-level quantity, reserved quantity, freeze state, and audit logs |
| Orders | Order header, line items, price snapshots, lifecycle state, cancellation |
| Customer balance | Current balance, reserved balance, and balance audit history |
| Stock opname | Spreadsheet-driven physical counts, discrepancy reviews, resolution actions |
| Bulk import | File metadata, processing status, chunk progress, row-level errors |
| Outbox | Durable event payloads, dispatch state, retries, and locking metadata |
| Idempotency | Request identity, request hash, processing state, and stored response |

## Database migrations

The PostgreSQL schema is built incrementally using Flyway migrations in [src/main/resources/db/migration](src/main/resources/db/migration). The migration history evolves the initial catalog and order schema into the current model, including variant-level inventory and order details, balance tracking, outbox dispatch, bulk import, stock-opname review, inventory freezing, and later enum/constraint changes.

When setting up a new environment, run the application with the intended database configuration so Flyway can apply the migrations in version order. Avoid editing migrations that have already been applied in a shared or deployed environment; add a new forward migration for subsequent schema changes.

## Local development

### Prerequisites

- JDK 17
- Maven (or the Maven wrapper, if included)
- PostgreSQL
- RabbitMQ
- AWS S3 configuration for file-upload workflows

### Run

~~~bash
git clone https://github.com/MarioSandy/inventory-management-springboot.git
cd inventory-management-springboot
./mvnw spring-boot:run
~~~

If the Maven wrapper is not included, run `mvn spring-boot:run`.

Configure the database, RabbitMQ, and S3 settings using the application configuration and environment variables. Do not commit real credentials, private keys, tokens, or production connection strings.

Run tests with:

~~~bash
./mvnw test
~~~

## Engineering considerations

- Keep inventory and balance changes in transactions with their corresponding audit records.
- Apply a consistent locking strategy to inventory operations that may race with orders or stock opname.
- Treat RabbitMQ as at-least-once delivery and make consumers idempotent.
- Monitor outbox retries, failed events, stale processing locks, and bulk-import progress.
- Keep API authorization aligned with the CUSTOMER, WAREHOUSE_STAFF, and USER_ADMIN responsibilities.
- Use Flyway forward migrations to evolve database schema safely.
