# State of the Project: Inventory API Spring Boot

## 1. Goals

Build a production-grade inventory and order management system that:

- Allows **Customers** to place orders against available inventory and reserved balance.
- Allows **Warehouse Managers** to manage master data, products, bulk product uploads,
  stock opname via Excel, and inventory movement operations.
- Allows **User Administrators** to manage Warehouse Manager registry and access control.
- Guarantees data security and integrity across **concurrent processes** using RabbitMQ
  and outbox-based reliable messaging.

## 2. Actors

- **Customer** — creates and manages orders; balance is reserved when an order is created.
- **Warehouse Manager** — owns inventory movement, stock opname review, order fulfillment,
  and product/master data authoring (including bulk Excel flows).
- **User Administrator** — manages Warehouse Manager registration and access management.

## 3. Feature Scope

- Create master data for product (brand, category, size, color).
- Create product (single and bulk via Excel).
- Create products using Excel upload.
- Inventory stock opname using Excel.
- Inventory movement operations (restock, damage, etc.).
- Create and manage order.

## 4. Architecture Overview

- **Spring Boot** REST API (`com.example.inventory_api_springboot`).
- **JPA/Hibernate** entities with auditing (`@EntityListeners(AuditingEntityListener.class)`),
  soft deletes via `@SQLRestriction("deleted_at IS NULL")`, and version/lock columns where relevant.
- **Outbox pattern** (`Outbox` entity + `OutboxRepository` + `OutboxService`) to publish reliable
  domain events to RabbitMQ.
- **RabbitMQ** queues for asynchronous, concurrent processing
  (e.g. `ORDER_PROCESSING_QUEUE`, outbox dispatch, stock opname chunk processing).
- **Native repositories** (`*NativeRepositoryImpl`) for high-throughput batch operations,
  bulk inserts, and complex SQL (locking, cursor pagination, `INSERT ... RETURNING`, `UPDATE ... FROM`).
- **Pessimistic locking** (`LockModeType.PESSIMISTIC_WRITE`, `jakarta.persistence.lock.timeout = -2`)
  and freeze flags on inventory to protect concurrent mutations.
- **S3** storage for uploaded Excel files.
- **Scheduled dispatchers**:
  - Order scheduler (fetch up to 500 PENDING orders, mark PROCESSING, emit outbox per order).
  - Outbox scheduler (fetch up to 100 PENDING outbox rows, push to RabbitMQ per row, mark DISPATCHED).
  - Stock opname batch scheduler for chunked processing.

## 5. Primary Workflows (current behavior)

### 5.1 Order Lifecycle

1. **Customer creates order**
   - Order created with status `PENDING`.
   - User balance is **reserved** (`UserBalance` / `UserBalanceLog` with `ORDER_RESERVED`).
2. **Order Scheduler**
   - Fetches up to **500** `PENDING` orders whose order items are not frozen.
   - Updates status to `PROCESSING`.
   - Emits one `Outbox` row (status `PENDING`) per order.
3. **Outbox Order Scheduler**
   - Fetches up to **100** `PENDING` outbox rows.
   - Pushes message to RabbitMQ per row and marks `DISPATCHED`.
4. **Order Processing Consumer** (`OrderProcessingConsumer`, concurrency = 5)
   - If any order item is frozen → revert to `PENDING` and exit.
   - If any inventory quantity insufficient → cancel order and refund balance.
   - If valid → reserve inventory, set order status to `ORDER_RESERVED`.
5. **Warehouse Manager fulfillment**
   - Fetches `ORDER_RESERVED` orders, transitions to `ORDER_SHIPPED`.
   - Deducts balance / reserve balance by order subtotal.
   - Deducts inventory quantity / reserved quantity by order detail quantity.

### 5.2 Inventory Stock Opname

1. **Warehouse Manager downloads** stock opname Excel template.
2. Uploads filled Excel → file stored to S3, `DataBulkImportLog` created, `Outbox` created (`PENDING`).
3. **Outbox scheduler** dispatches to RabbitMQ.
4. **File Orchestrator (consumer)**
   - Fetches Excel from S3.
   - Fetches inventory by product variant, sets `freeze = true`, validates.
   - Invalid rows → `DataBulkImportLogError` (row + error).
   - Every **500** rows → batch valid rows, create **outbox batch stock opname** and commit.
5. **Stock Opname Scheduler (consumer)**
   - Validates each row against master data; invalid → `DataBulkImportLogError`.
   - Compares actual vs. system quantity:
     - Equal → no-op, unfreeze.
     - Actual > system → update immediately, unfreeze.
     - Actual < system → create `StockOpnameReview` (still frozen until reviewed).
6. **Warehouse Manager review**
   - **Approve**
     - `RECONCILE_TO_ACTUAL` → update quantity to actual, unfreeze.
     - `CORRECT_INVENTORY_MOVEMENT` → requires matching manual inventory movement
       (Damage/Restock) whose total equals the opname difference, then unfreeze.
     - `RESOLVE_RESERVATION_SHORTAGE` → update to actual, then cancel the **latest**
       `ORDER_RESERVED` order details (single detail cancellation, not entire order)
       until shortage is covered, refund the balance accordingly, then unfreeze.
   - **Reject**
     - Records `root_cause`, `investigation_note`, `resolution_action = REJECT`, unfreezes.

## 6. What Is Currently Working

- REST endpoints for order creation, product size dropdown, and supporting lookups
  (order, size).
- Domain entities for users, user balance + logs, inventory + logs, orders + details,
  product hierarchy, stock opname review, outbox, data bulk import logs + errors,
  pending inventory process, sessions.
- Outbox pattern with dispatch, retry, and failure paths
  (`updateStatusForRetryDispatch`, `updateStatusFailedDispatch`, `updateStatusSuccessDispatch`).
- Order processing consumer with concurrency, reservation, and cancellation logic.
- Bulk product insert path (`insertBulkProducts`, `insertBulkProductVariants`).
- Bulk product Excel chunking via `FileOrchestratorService` + outbox.
- Inventory stock opname Excel generation, upload, chunking, and validation.
- Stock opname review entity + inventory mutation with reservation-shortage flow
  (partially wired; see "In Progress").
- S3 upload integration (`S3Service`).
- Pessimistic locking on user balance and inventory freeze updates.

## 7. Currently In Progress / Next Up

- Completing the **stock opname review resolution** flow end-to-end:
  - Ensuring `RESOLVE_RESERVATION_SHORTAGE` cancels the correct latest `ORDER_RESERVED`
    order details with correct balance refund amounts.
  - Guaranteeing `CORRECT_INVENTORY_MOVEMENT` only unfreezes once the matching
    inventory movement total equals the opname difference.
- Hardening concurrency:
  - Verify freeze/unfreeze consistency between File Orchestrator, Stock Opname Scheduler,
    and the review flow.
  - Verify locks and timeouts under concurrent consumer load.
- Order lifecycle edge cases:
  - Freeze interaction between order processing and stock opname.
  - Partial cancellation rollback and refund accuracy.
- Strengthening observability of outbox dispatch (retry vs. failure thresholds).
- Expanding test coverage for concurrent flows.

## 8. Conventions & Patterns

- Soft delete via `deleted_at` + `@SQLRestriction`.
- Auditing fields via `@EntityListeners(AuditingEntityListener.class)`.
- Bulk operations through native repositories (`@Transactional` + `@Modifying` where applicable).
- Event emission **always** via `OutboxService.create(...)`; never direct queue publish outside
  the outbox dispatcher.
- Enum-based validation for balance + inventory log deltas
  (`UserBalanceLogTypeEnum`, `InventoryLogTypeEnum`) ensures invariant sign rules.
- Role enforcement via `@RequireRole(UserRoleEnum.CUSTOMER)` (and equivalents).

## 9. How To Use This Context

When starting a new change, paste the GitHub Issue link that contains this document.
That link gives the model:
- Full project intent and actor model.
- Feature inventory.
- Current workflow expectations for ordering and stock opname.
- What is already shipped vs. what is still in progress.
- Coding conventions to remain consistent with.
