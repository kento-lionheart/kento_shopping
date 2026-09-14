# Kento Shopping — E-Commerce REST API

A backend REST API for an e-commerce platform built with Spring Boot. Covers the full shopping flow from user registration to order placement and payment.
Kento
---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.14 |
| Security | Spring Security 6 + JWT (jjwt 0.12.6) |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI 2.8.8 (Swagger UI) |
| Build | Maven |

---

## Features / Use Cases

### Authentication
| UC | Feature | Status |
|----|---------|--------|
| UC-01 | Register account | ✅ Done |
| UC-02 | Login with JWT | ✅ Done |
| UC-03 | Logout | ✅ Done (client-side) |

### Customer
| UC | Feature | Status |
|----|---------|--------|
| UC-04 | Manage delivery address | ✅ Done |
| UC-05 | Browse products (paginated) | ✅ Done |
| UC-06 | Search and filter products | ✅ Done |
| UC-07 | View product details + stock status | ✅ Done |
| UC-08 | Manage cart | ✅ Done |
| UC-09 | Checkout and place order | ✅ Done |
| UC-10 | Pay for an order with coins | ✅ Done |
| UC-11 | View order history | ✅ Done |
| UC-12 | Cancel order (unpaid only) | ✅ Done |
| UC-16 | Request coin top-up | ✅ Done |
| UC-18 | View wallet and ledger | ✅ Done |

### Admin
| UC | Feature | Status |
|----|---------|--------|
| UC-13 | Manage products and inventory | ✅ Done |
| UC-14 | Manage orders and statuses | ✅ Done |
| UC-15 | Manage roles and permissions | ✅ Done |
| UC-17 | Review coin top-up requests | ✅ Done |
| UC-19 | Configure and run a flash sale | 🚧 Create / edit / cancel done; activation pending |

### Coins

Coins are the only way to pay — 1 coin = 1 VND, so catalogue prices are unchanged. There is no payment gateway: coins exist only when an admin approves a customer's top-up request, and the approving admin is recorded against it.

A wallet keeps a materialised balance; the real record is an append-only ledger, both written in the same transaction so `SUM(ledger) == balance` always holds. Staff and admins have no wallet at all — an account that can create coins must not be able to spend them.

### Access control

Permission-based RBAC. `User` → many `Role` → many `Permission`, enforced with `@PreAuthorize` on permissions rather than roles.

| Role | Scope |
|------|-------|
| `CUSTOMER` | Shops. Holds no permissions — access comes from authentication plus ownership |
| `PRODUCT_STAFF` | Catalogue and stock |
| `ORDER_STAFF` | Fulfilment and support; can read every wallet, can mint no coins |
| `FLASHSALE_MANAGER` | Campaigns; needs catalogue write access because activating a sale carves stock out of inventory |
| `ADMIN` | Everything — and the only role that cannot shop |

Admins and staff are barred from cart, orders and addresses: an account that can approve coin top-ups must never be able to spend them. Staff who also shop hold two roles.

---

## API Overview

### Auth
```
POST   /api/v1/auth/register                    Register a new account
POST   /api/v1/auth/login                       Login — returns JWT, roles and permissions
GET    /api/v1/auth/me                          Re-read own identity and authorities
```

### Address
```
GET    /api/v1/addresses                        Get current user's address
POST   /api/v1/addresses                        Create address
PUT    /api/v1/addresses                        Update address
```

### Products
```
GET    /api/v1/products                         Browse / search / filter products
GET    /api/v1/products/{id}                    View product details
GET    /api/v1/categories                       List all categories
```

### Cart
```
GET    /api/v1/cart                             View cart
POST   /api/v1/cart/items                       Add item to cart
PUT    /api/v1/cart/items/{productId}           Update item quantity
DELETE /api/v1/cart/items/{productId}           Remove item from cart
DELETE /api/v1/cart                             Clear cart
```

### Orders
```
POST   /api/v1/orders/checkout                  Place an order
POST   /api/v1/orders/{orderId}/payment         Pay with coins — no request body
GET    /api/v1/orders                           View order history
PATCH  /api/v1/orders/{orderId}/cancel          Cancel a pending order
```

### Wallet
```
GET    /api/v1/wallet                           Balance + 10 most recent transactions
GET    /api/v1/wallet/transactions               Paginated ledger
POST   /api/v1/wallet/top-ups                   Request coins (10,000 – 50,000,000)
GET    /api/v1/wallet/top-ups                   Own top-up requests
DELETE /api/v1/wallet/top-ups/{id}              Withdraw a pending request
```

### Admin
```
POST   /api/v1/admin/products                   Create product
PUT    /api/v1/admin/products/{id}              Update product
PUT    /api/v1/admin/products/{id}/stock        Update stock
DELETE /api/v1/admin/products/{id}              Delete product

GET    /api/v1/admin/orders                     List all orders (filter by email/status)
PUT    /api/v1/admin/orders/{id}/status         Update order status

GET    /api/v1/admin/users                      List users and their roles
GET    /api/v1/admin/users/{id}                 View one user
PUT    /api/v1/admin/users/{id}/roles           Replace a user's roles
GET    /api/v1/admin/roles                      List roles
POST   /api/v1/admin/roles                      Compose a new role
PUT    /api/v1/admin/roles/{id}/permissions     Edit what a role grants
GET    /api/v1/admin/permissions                The permission catalogue (read-only)

GET    /api/v1/admin/flash-sales                List sales (filter by status)
GET    /api/v1/admin/flash-sales/{id}           View one sale
POST   /api/v1/admin/flash-sales                Schedule a sale for one product
PUT    /api/v1/admin/flash-sales/{id}           Edit a scheduled sale
PUT    /api/v1/admin/flash-sales/{id}/cancel    Cancel a scheduled sale
DELETE /api/v1/admin/flash-sales/{id}           Delete a scheduled or cancelled sale
```

Each admin endpoint requires a specific permission, not merely an admin role — see the matrix in the design docs.

Full interactive docs available at `/swagger-ui/index.html` when the app is running.

---

## Running Locally

**Prerequisites:** Java 17, MySQL

1. Create a MySQL database named `kento_shopping`
2. Configure credentials in `src/main/resources/application-dev.yaml`
3. Run:

```bash
./mvnw spring-boot:run
```

API is available at `http://localhost:8080`
Swagger UI at `http://localhost:8080/swagger-ui/index.html`

Sample data is seeded automatically on first run in `dev` profile — see `DataSeeder.java` for credentials.