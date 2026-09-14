# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Kento Shopping — an e-commerce REST API (Spring Boot 3.5.14, Java 17, MySQL, JWT). `README.md` holds the full endpoint list and use-case status table; keep it updated when adding endpoints.

## Commands

```bash
./mvnw spring-boot:run          # run on :8080 (dev profile is active by default)
./mvnw clean package            # build jar
./mvnw test                     # run all tests
./mvnw test -Dtest=ClassName#methodName   # run a single test
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Requires a local MySQL database named `kento_shopping` and a Redis on `localhost:6379` (flash-sale stock). `ddl-auto: update` — Hibernate manages the schema, there are no migration files.

## Configuration

`application-dev.yaml` is **gitignored** and must exist locally. It holds the datasource credentials *and* `jwt.secret` / `jwt.expiration` — the base `application.yaml` does not define them, so any new profile must supply both or `JwtUtil` fails to start.

`DataSeeder` (`@Profile("dev")`) seeds the permission catalogue, roles, categories, ~43 products with inventory, users, wallets, and sample orders on first run only (`if (userRepository.count() > 0) return;`). Seed order matters: permissions → roles → role/permission links → users → wallets → orders.

Each of the 10 customers gets a wallet holding 100,000,000 coins, credited through an `APPROVED` `TOP_UP_REQUEST` rather than by setting the balance directly, plus 3 `PENDING` requests so the admin review queue is not empty. Seeded orders whose payment is `SUCCESS` debit real coins, so `SUM(ledger) == balance` holds from the very first run instead of only after the first live payment.

Because it bails on a populated database and `ddl-auto: update` never drops columns, any schema change to the role model means dropping and recreating `kento_shopping` rather than migrating. Flyway is deferred until pre-deploy.

Seeded accounts, all with password `Kiet123456`:

| Account | Roles |
| ------- | ----- |
| `admin@kento.com` | `ADMIN` |
| `product.staff@kento.com` | `PRODUCT_STAFF` |
| `order.staff@kento.com` | `ORDER_STAFF` |
| `flashsale@kento.com` | `FLASHSALE_MANAGER` |
| `nguyen.van.an@gmail.com` | `CUSTOMER` + `ORDER_STAFF` — deliberate dual-role fixture |
| 9 other `@gmail.com` customers | `CUSTOMER` |

`CUSTOMER` holding zero permissions is correct, not a seeding bug: a customer's access comes from being authenticated plus owning the row. Product images are served from `src/main/resources/static/images/products/<category>/`, referenced by `imageUrl` paths like `/images/products/electronics/iphone15pro.png`.

## Architecture

Layering is strict: `controller` → `service` (interface) → `service.impl` → `repository`. Controllers never touch repositories or entities directly; every response goes out as a DTO built by a private `mapTo…Response` method inside the service impl.

**Security.** `User` *is* the `UserDetails` implementation, so controllers take `@AuthenticationPrincipal User user` and services receive the entity directly — there is no separate lookup-by-email step in business code. `JwtAuthFilter` validates the bearer token and populates the SecurityContext; it never rejects a request itself (invalid/missing token just falls through to the authorization rules in `SecurityConfig`). Admin endpoints live under `controller/admin/` with the matching `dto/request/admin/` package. Ownership checks (does this order belong to this user?) are done inside the service, throwing `IllegalArgumentException`.

**RBAC.** Authorization is permission-based via `@PreAuthorize("hasAuthority('…')")` on controller methods. `SecurityConfig` keeps only coarse rules: public paths, `hasRole("CUSTOMER")` on the shopping endpoints, and `/api/v1/admin/**` merely `authenticated()` as a first gate — which admin may do what is decided by the annotations.

`User` holds many `Role`s (`USER_ROLE`), each granting many `Permission`s (`ROLE_PERMISSION`); there is no `role` column. `getAuthorities()` emits permission names *and* `ROLE_<name>` for each role, so both `hasAuthority` and `hasRole` work. Permissions are a **closed set** defined by the `PermissionName` enum — a name outside it is rejected, since only a `@PreAuthorize` literal gives a permission meaning. Roles are rows and may be composed at runtime through `ROLE_MANAGE`.

Authorities are never put in the JWT: `CustomUserDetailsService` reloads them per request via `UserRepository.findByEmailWithAuthorities`, so a role change takes effect on the victim's next request with no token refresh. Two consequences to respect — that query must join-fetch `roles` and `roles.permissions` (`getAuthorities()` runs after the persistence context closes), and both collections must be `Set`, not `List`, or Hibernate throws `MultipleBagFetchException`.

**Separation of duties.** Admins and staff cannot shop — cart, orders and addresses require `ROLE_CUSTOMER`. An account that can approve coin top-ups must never be able to spend them. Staff who also shop hold two roles; the seeder includes one such account deliberately. `ROLE_ASSIGN` (hand out existing roles) is split from `ROLE_MANAGE` (invent roles, hence effectively root) — that split is the privilege-escalation boundary.

`GlobalExceptionHandler` must keep its `AccessDeniedException` handler: without it the catch-all `Exception` handler turns every `@PreAuthorize` denial into a 500.

CORS is hardcoded to `http://localhost:5173` in `SecurityConfig` for the frontend dev server.

**Errors.** All exceptions are custom unchecked types in `exception/`, each with an explicit handler in `GlobalExceptionHandler` mapping to an `ErrorResponse(status, message)`. `IllegalArgumentException` is the catch-all for business-rule violations (→ 400); a bare `Exception` handler returns a generic 500. When adding a new failure mode, add both the exception class and its handler — an unhandled one silently becomes "An unexpected error occurred".

**Entities.** All extend `BaseEntity` (IDENTITY id, `@CreationTimestamp` / `@UpdateTimestamp`). `Inventory` is a separate `@OneToOne @MapsId` entity sharing the product's PK, with an `@Version` optimistic-lock column — stock lives there, never on `Product`. Orders snapshot data at purchase time (`OrderItem.productName`, `priceAtPurchase`, and the flattened `ship*` address fields on `Order`) so later product/address edits don't rewrite history.

**Order lifecycle.** `checkout` validates stock → computes subtotal + flat 30,000 shipping → creates `Order(PENDING)` + `OrderItem`s → decrements `Inventory.quantity` → clears the cart. `cancelOrder` restores inventory and is only allowed from `PENDING`. `makePayment` takes no request body — `COIN` is the only method — and debits the wallet, appends a `PURCHASE` ledger row, creates the `Payment` and sets the order `PAID`, all in one transaction. Insufficient balance throws `InsufficientBalanceException` (→ 400) and leaves the order `PENDING`, so the customer can top up and pay the order they already placed. Admin `updateOrderStatus` refuses to change `DELIVERED` or `CANCELLED` orders, and refuses `CANCELLED` as a *target* — setting it there would skip the restock and the coin refund, silently keeping the customer's money. All of these rely on JPA dirty checking inside `@Transactional` rather than explicit `save()` calls.

**Wallet and coins.** 1 coin = 1 VND, so catalogue prices are unchanged. `WALLET` holds a materialised balance with an `@Version` column; `COIN_TRANSACTION` is the append-only ledger and the real record. `WalletServiceImpl.write` is the only place a balance moves, and it writes both rows in one transaction — that is what makes `SUM(ledger.amount) == wallet.balance` an invariant you can check. Never mutate `Wallet.balance` outside `WalletService.credit` / `debit`.

Wallets are created lazily by `WalletService.getOrCreate`, guarded by a unique constraint on `user_id` (a lost race is caught as `DataIntegrityViolationException` and re-read). That covers seeded, registered, and later-granted customers without any of those paths knowing about wallets. Staff and admins never have one — `/api/v1/wallet/**` requires `ROLE_CUSTOMER`, which is what keeps approving coins and spending them in separate accounts.

Coins exist only because an admin approved a `TOP_UP_REQUEST`; `reviewedBy` records who. Re-checking `PENDING` inside the approval transaction is what stops two admins crediting the same request twice.

**Flash sale.** One product per sale, no cart, no cancel or refund. The request path decides everything in Redis and never waits for MySQL; the database is written afterwards by a stream consumer.

- *Lifecycle.* `FlashSaleScheduler` ticks every second through `FlashSaleLifecycleService` (a separate bean, so each sale gets its own `@Transactional` proxy call). Activation subtracts `allocatedQty` from `Inventory` in MySQL and writes `flashsale:{id}:stock` only *after commit*, with `SET NX` — a lost Redis write yields an unbuyable sale, never an oversold one, and the restore step re-creates a missing key. Close is two-phase: `CLOSING` deletes the stock key, and settlement returns `allocatedQty − soldQty` to `Inventory` only once `flashsale:{id}:inflight` is 0.
- *Claim.* `resources/redis/flashsale-claim.lua` checks and decrements stock, adds the quantity to `inflight`, writes the claim hash and `XADD`s to `flashsale:claims` in one atomic call — nothing can be claimed without being published. The endpoint returns `202 PROCESSING` with a `claimId`; claim state lives only in Redis (24 h TTL), and a failed claim never writes an `Order`.
- *Consumer.* `FlashSaleClaimConsumer` reads the stream as group `flashsale-orders` with manual ack. `FlashSaleOrderService.persistClaim` writes order, item, `PURCHASE` debit, payment and `soldQty` in one transaction. Idempotency comes from `Payment.transactionId = claimId` (already unique) plus `flashsale-finalize.lua`, which only acts on a claim still `PROCESSING`. Business rejections (no address, not enough coins) throw `FlashSaleClaimRejectedException` *before* any write and finalise the claim `FAILED`, returning the stock. Anything else leaves the message pending; a 5-second job re-claims idle entries and gives up after 5 deliveries. Processing is `synchronized`, which is what makes the listener and the retry job safe to share one message.
- *Invariant.* `allocatedQty = stock + inflight + soldQty`. `inflight` counts units, not claims.
- *Schema trap.* `status` columns backed by `@Enumerated(STRING)` are MySQL `ENUM`s; `ddl-auto: update` will not add a new constant, so extending `FlashSaleStatus` needs an `ALTER TABLE … MODIFY`.

**Product listing.** `ProductServiceImpl.getProducts` composes `Specification`s (name/description LIKE, category equals) via `Specification.allOf`; sorting and pagination are parsed in `ProductController` (`sort=newest|price_asc|price_desc`, default page size 12).

## Tests

`src/test/java/.../controller/` and `service/` exist but are empty — only `KentoShoppingApplicationTests` is present. `spring-security-test` is on the classpath.