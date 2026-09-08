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

Requires a local MySQL database named `kento_shopping`. `ddl-auto: update` — Hibernate manages the schema, there are no migration files.

## Configuration

`application-dev.yaml` is **gitignored** and must exist locally. It holds the datasource credentials *and* `jwt.secret` / `jwt.expiration` — the base `application.yaml` does not define them, so any new profile must supply both or `JwtUtil` fails to start.

`DataSeeder` (`@Profile("dev")`) seeds categories, ~43 products with inventory, users, and sample orders on first run only (`if (userRepository.count() > 0) return;`). Seeded admin is `admin@kento.com` / `Kiet123456`. Product images are served from `src/main/resources/static/images/products/<category>/`, referenced by `imageUrl` paths like `/images/products/electronics/iphone15pro.png`.

## Architecture

Layering is strict: `controller` → `service` (interface) → `service.impl` → `repository`. Controllers never touch repositories or entities directly; every response goes out as a DTO built by a private `mapTo…Response` method inside the service impl.

**Security.** `User` *is* the `UserDetails` implementation, so controllers take `@AuthenticationPrincipal User user` and services receive the entity directly — there is no separate lookup-by-email step in business code. `JwtAuthFilter` validates the bearer token and populates the SecurityContext; it never rejects a request itself (invalid/missing token just falls through to the authorization rules in `SecurityConfig`). Authorization is URL-based in `SecurityConfig`, not annotation-based: `/api/v1/admin/**` requires `ROLE_ADMIN`, GET on products/categories and `/api/v1/auth/**` are public, everything else authenticated. Admin endpoints live under `controller/admin/` with the matching `dto/request/admin/` package. Ownership checks (does this order belong to this user?) are done inside the service, throwing `IllegalArgumentException`.

CORS is hardcoded to `http://localhost:5173` in `SecurityConfig` for the frontend dev server.

**Errors.** All exceptions are custom unchecked types in `exception/`, each with an explicit handler in `GlobalExceptionHandler` mapping to an `ErrorResponse(status, message)`. `IllegalArgumentException` is the catch-all for business-rule violations (→ 400); a bare `Exception` handler returns a generic 500. When adding a new failure mode, add both the exception class and its handler — an unhandled one silently becomes "An unexpected error occurred".

**Entities.** All extend `BaseEntity` (IDENTITY id, `@CreationTimestamp` / `@UpdateTimestamp`). `Inventory` is a separate `@OneToOne @MapsId` entity sharing the product's PK, with an `@Version` optimistic-lock column — stock lives there, never on `Product`. Orders snapshot data at purchase time (`OrderItem.productName`, `priceAtPurchase`, and the flattened `ship*` address fields on `Order`) so later product/address edits don't rewrite history.

**Order lifecycle.** `checkout` validates stock → computes subtotal + flat 30,000 VND shipping → creates `Order(PENDING)` + `OrderItem`s → decrements `Inventory.quantity` → clears the cart. `cancelOrder` restores inventory and is only allowed from `PENDING`. `makePayment` marks the order `PAID` only for `MOMO`; other methods leave the payment `PENDING`. Admin `updateOrderStatus` refuses to change `DELIVERED` or `CANCELLED` orders. All of these rely on JPA dirty checking inside `@Transactional` rather than explicit `save()` calls.

**Product listing.** `ProductServiceImpl.getProducts` composes `Specification`s (name/description LIKE, category equals) via `Specification.allOf`; sorting and pagination are parsed in `ProductController` (`sort=newest|price_asc|price_desc`, default page size 12).

## Tests

`src/test/java/.../controller/` and `service/` exist but are empty — only `KentoShoppingApplicationTests` is present. `spring-security-test` is on the classpath.