# Kento Shopping — E-Commerce REST API

A backend REST API for an e-commerce platform built with Spring Boot. Covers the full shopping flow from user registration to order placement and payment.

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
| UC-03 | Logout | — |

### Customer
| UC | Feature | Status |
|----|---------|--------|
| UC-04 | Manage delivery address | ✅ Done |
| UC-05 | Browse products (paginated) | ✅ Done |
| UC-06 | Search and filter products | ✅ Done |
| UC-07 | View product details + stock status | ✅ Done |
| UC-08 | Manage cart | 🔄 In progress |
| UC-09 | Checkout and place order | — |
| UC-10 | Make payment | — |
| UC-11 | View order history | — |
| UC-12 | Cancel order | — |

### Admin
| UC | Feature | Status |
|----|---------|--------|
| UC-13 | Manage products and inventory | — |
| UC-14 | Manage orders and statuses | — |

---

## API Overview

```
POST   /api/v1/auth/register         Register a new account
POST   /api/v1/auth/login            Login and receive JWT

GET    /api/v1/addresses             Get current user's address
POST   /api/v1/addresses             Create address
PUT    /api/v1/addresses             Update address

GET    /api/v1/products              Browse / search / filter products
GET    /api/v1/products/{id}         View product details

POST   /api/v1/admin/products        Create product (ADMIN)
PUT    /api/v1/admin/products/{id}   Update product (ADMIN)
DELETE /api/v1/admin/products/{id}   Delete product (ADMIN)
```

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