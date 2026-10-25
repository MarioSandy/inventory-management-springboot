# Inventory Management API — Spring Boot

A Java backend for inventory, customer orders, balance reservation, bulk product imports, and Excel-based stock opname. The project demonstrates PostgreSQL persistence, Spring Security, RabbitMQ-based asynchronous processing, and the transactional outbox pattern.

> **Schema warning:** The checked-in Flyway migration and current JPA entities are not fully aligned. Read the database schema section before creating a fresh database. This README records that discrepancy instead of presenting an inferred schema as a verified migration.

## Technology stack

- Java 17
- Spring Boot 4.1.1
- Spring Data JPA / Hibernate
- PostgreSQL and Flyway
- Spring Security and OAuth2 Authorization Server starter
- RabbitMQ via Spring AMQP
- AWS S3 for uploaded spreadsheets
- Apache POI and Excel Streaming Reader
- Maven and Lombok

See [pom.xml](pom.xml) for dependencies.

## Domain capabilities

- User accounts, role assignment, and session tracking
- Product catalog organized by category and brand
- Product variants based on color and size, with generated SKU support
- Inventory quantity and reserved-quantity tracking
- Customer orders and order details with stored unit-price snapshots
- User balance and balance audit logs
- Inventory movement/audit logs
- Bulk product import from Excel, with per-row error tracking
- Stock opname spreadsheet upload, chunked processing, freeze protection, and human review
- Outbox-backed asynchronous events dispatched to RabbitMQ
- Idempotency-key persistence for request deduplication

Some workflows are documented as partially implemented or in progress. See [VIBE_CONTEXT.md](VIBE_CONTEXT.md); do not assume every listed workflow is production-complete.

## Architecture

~~~mermaid
flowchart LR
    Client[Client / REST API] --> API[Spring Boot application]
    API --> PG[(PostgreSQL)]
    API --> S3[(Amazon S3)]
    PG --> Scheduler[Scheduled dispatchers]
    Scheduler --> Outbox[(Outbox rows)]
    Outbox --> Rabbit[RabbitMQ]
    Rabbit --> Consumer[Async consumers]
    Consumer --> PG
~~~

### Reliable asynchronous processing

The application records work in an outbox table as part of the database transaction. A scheduled dispatcher publishes pending events to RabbitMQ and updates dispatch status. Consumers must tolerate duplicate delivery and make processing idempotent; database state remains the source of truth.

### Order lifecycle

1. A customer creates an order. The order is initially PENDING, and the required balance is reserved.
2. A scheduler claims eligible pending orders and changes them to PROCESSING, creating outbox records.
3. The outbox dispatcher publishes messages to RabbitMQ.
4. The order consumer checks inventory freeze state and availability. Frozen orders are deferred; insufficient inventory follows the failure/cancellation and refund path.
5. Successful orders transition to the reserved state used by the fulfillment flow.
6. Warehouse fulfillment ships the order and updates reserved balance and inventory.

The exact enum values in the initial migration do not currently match all states described by the entity/context code. Resolve the schema mismatch before relying on this flow in a clean environment.

### Stock opname flow

1. A warehouse manager downloads and fills an Excel template.
2. The upload is stored in S3 and tracked by a bulk-import log and an outbox event.
3. A consumer reads the file and processes it in chunks, validating rows and freezing the relevant inventory.
4. Rows where physical quantity equals system quantity need no quantity adjustment; higher quantities can be reconciled automatically according to the documented workflow.
5. Lower physical quantities create a stock-opname review that remains frozen until a reviewer resolves it.
6. Review resolution can reconcile to actual quantity, request/reject a recount, or resolve a reservation shortage. Check the current enum and service implementation for the exact supported actions.

## Data model

~~~mermaid
erDiagram
    USER ||--o| USER_BALANCE : owns
    USER ||--o{ USER_BALANCE_LOG : has
    USER ||--o{ USER_SESSION : has
    USER ||--o{ ORDER : places
    USER ||--o{ INVENTORY_LOG : creates
    USER ||--o{ OUTBOX : creates
    PRODUCT_CATEGORY ||--o{ PRODUCT : classifies
    PRODUCT_BRAND ||--o{ PRODUCT : brands
    PRODUCT ||--o{ PRODUCT_VARIANT : has
    PRODUCT_COLOR ||--o{ PRODUCT_VARIANT : colors
    PRODUCT_SIZE ||--o{ PRODUCT_VARIANT : sizes
    PRODUCT_VARIANT ||--o| INVENTORY : tracks
    PRODUCT_VARIANT ||--o{ INVENTORY_LOG : records
    PRODUCT_VARIANT ||--o{ ORDER_DETAIL : ordered
    PRODUCT_VARIANT ||--o{ STOCK_OPNAME_REVIEW : reviewed
    ORDER ||--|{ ORDER_DETAIL : contains
    DATA_BULK_IMPORT_LOG ||--o{ DATA_BULK_IMPORT_LOG_ERROR : records
~~~

This diagram summarizes entity relationships, not a guarantee that the current Flyway migration creates every listed table or foreign key.

### Entities and tables represented in source

| Table / entity | Purpose |
|---|---|
| user / User | User identity, email, password, role, and soft deletion |
| user_role / role mapping | Role assignment; the initial migration also declares a PostgreSQL role enum |
| user_session / UserSession | Client session and token metadata |
| user_balance / UserBalance | Current and reserved monetary balance |
| user_balance_log / UserBalanceLog | Before/after and delta audit for balance mutations |
| product_category / ProductCategory | Product category master data |
| product_brand / ProductBrand | Product brand master data |
| product_color / ProductColor | Color master data |
| product_size / ProductSize | Size master data |
| product / Product | Product-level details and base price |
| product_variant / ProductVariant | Sellable variant, SKU, price, color, and size |
| inventory / Inventory | Quantity, reserved quantity, and freeze state |
| inventory_log / InventoryLog | Quantity and reserved-quantity before/after/delta, event type, reference, and actor |
| order / Order | Order header and lifecycle status |
| order_detail / OrderDetail | Variant, quantity, unit price, subtotal, and cancellation state |
| stock_opname_review / StockOpnameReview | Physical-count discrepancy and review resolution |
| outbox / Outbox | Reliable event publication state and payload |
| data_bulk_import_log / DataBulkImportLog | Uploaded file and chunk-processing progress |
| data_bulk_import_log_error / DataBulkImportLogError | Row-level import errors; composite key includes import ID and row number |
| idempotency_key / idempotency key model | Tracks request hash, processing status, and stored response |

The full column types and constraints must be verified against both migration SQL and entity mappings. The list above is an inventory of the domain model, not a substitute for executable DDL.

## Database schema and migration status

Flyway migrations live in src/main/resources/db/migration. The initial checked-in migration is [V1__init.sql](src/main/resources/db/migration/V1__init.sql).

**Known discrepancies found during repository review:**

- The migration creates inventory.product_id referencing product.id; the current Inventory entity uses product_variant_id and maps inventory to ProductVariant.
- The migration creates order_detail.product_id referencing product.id; the current OrderDetail entity uses product_variant_id and maps to ProductVariant.
- The migration's order_status_enum contains PENDING, PROCESSING, COMPLETED, and FAILED; the project context describes additional reserved/shipped lifecycle states.
- The initial migration does not define all tables represented by current entity classes, including balance logs, inventory logs, outbox, bulk-import tracking, and stock-opname review.
- Entity fields such as deleted_at, version, timestamps, and other constraints are not consistently represented in the initial migration.
- The migration defines user_role as a separate role mapping table, while the current User entity has a role field mapped directly on the user entity. Confirm which design is intended before generating a replacement migration.
- The migration defines product_brand.id as CHAR(4); the current entity uses a UUID ID and a separate code field.
- The initial migration defines idempotency_key.id as a non-generated BIGINT and uses a key column; check the current model and repository implementation before relying on it.

**Recommended next step:** reconcile these differences in a deliberate Flyway migration (or revise the entity mappings to match the intended database). Do not simply edit an already-applied migration in an environment with existing data. For an existing database, add a forward migration and plan any data conversion/backfill explicitly.

## Local development

Prerequisites:

- JDK 17
- Maven
- PostgreSQL
- RabbitMQ
- AWS S3 bucket and credentials if using file upload workflows

1. Clone the repository:

   ~~~bash
   git clone https://github.com/MarioSandy/inventory-management-springboot.git
   cd inventory-management-springboot
   ~~~

2. Configure database, RabbitMQ, and AWS S3 settings using the application's configuration and environment variables. Never commit real credentials, access tokens, private keys, or production connection strings.

3. Run the application:

   ~~~bash
   ./mvnw spring-boot:run
   ~~~

   If the Maven wrapper is not present, use mvn spring-boot:run.

4. Run tests:

   ~~~bash
   ./mvnw test
   ~~~

The repository's exact configuration keys and startup requirements should be checked in the current application configuration before deployment.

## Security and operational notes

- Use environment variables or a secret manager for database passwords, signing keys, and AWS credentials.
- Keep access tokens and session identifiers out of logs.
- Ensure inventory freeze checks are enforced by every inventory mutation path.
- Treat RabbitMQ delivery as at-least-once; consumers should be idempotent.
- Monitor outbox retries, dead-letter/failure paths, stuck processing records, and stock-opname jobs.
- Test reservation, cancellation/refund, and freeze/unfreeze paths under concurrent transactions.
- Use forward-only Flyway migrations for deployed environments.

## Project context

See [VIBE_CONTEXT.md](VIBE_CONTEXT.md) for feature scope, workflow notes, implementation status, and conventions.
