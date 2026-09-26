# BajriX Marketplace

A small multi-seller construction / home-building product marketplace. Buyers browse and compare offers; sellers manage their own listings (price, stock, MOQ, stop/resume selling).

## 1. Project overview

One **Product** (e.g. "OPC 53 Grade Cement") can be offered by many **Seller Listings**, each with its own price, stock, minimum order quantity (MOQ) and status. The marketplace shows buyers every approved seller's active offer per product and lets each seller manage only their own listings.

## 2. Architecture

```
React SPA (Vite)  --HTTP/JSON-->  Spring Boot REST API  --JPA-->  PostgreSQL
        |                              |
   Vite dev proxy                Flyway migrations
   (/api -> :8080)               (V1 schema, V2 seed)
```

- `backend/marketplace` — Spring Boot 4.1.1, Java 21, Maven
- `frontend` — React 18 + Vite + Axios + React Router, plain CSS
- Layering: `controller` -> `service` -> `repository` -> `model`, with `dto` for request/response contracts and `exception` for error mapping.

## 3. Tech stack

| Layer    | Technology |
|----------|------------|
| Backend  | Java 21, Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Flyway, Actuator), Lombok |
| Database | PostgreSQL |
| Frontend | React 18, Vite, Axios, React Router 6 |
| Build    | Maven (backend), npm/Vite (frontend) |

## 4. How to run PostgreSQL

Any local PostgreSQL 12+ instance on `localhost:5432` works. This project was developed against a local install whose `psql` lives outside PATH; adjust to your installation.

Create the two databases used by the project:

```sql
CREATE DATABASE bajrix;        -- development
CREATE DATABASE bajrix_test;   -- integration tests (never use bajrix for tests)
```

## 5. How to configure the database

Edit `backend/marketplace/src/main/resources/application.properties` (development) and `backend/marketplace/src/test/resources/application-test.properties` (tests):

```
spring.datasource.url=jdbc:postgresql://localhost:5432/bajrix
spring.datasource.username=<your user>
spring.datasource.password=<your password>
```

Never commit real passwords to documentation; the properties files are the single place credentials live locally. Flyway runs automatically on startup and creates the schema plus seed data in whichever database the active profile points to.

## 6. How to run the backend

```bash
cd backend/marketplace
./mvnw.cmd spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

The API is served on `http://localhost:8080`. Health check: `GET /actuator/health`.

## 7. How to run the frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080` (see `vite.config.js`), so no CORS configuration is needed in development. Production build: `npm run build`.

Routes: `/products` (browse), `/products/:id` (product detail), `/seller` (seller dashboard), `/admin` (admin portal: summary counts, seller table, per-seller listings).

## 8. How to run tests

```bash
cd backend/marketplace
./mvnw.cmd clean test
```

Tests run with the `test` profile (`@ActiveProfiles("test")`) against `bajrix_test`. Flyway migrates that database before the suite; each test wipes and re-creates its own deterministic fixtures in `@BeforeEach`. The development database is never touched by tests.

## 9. API overview

Buyer endpoints:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/products?search=&category=&page=&size=&sort=` | Paginated product list with `lowestPrice` and `activeSellerCount` (DB-side search/filter) |
| GET | `/api/products/categories` | Distinct category names for the filter dropdown |
| GET | `/api/products/{id}` | Single product with offer summary |
| GET | `/api/products/{id}/listings` | Active offers from APPROVED sellers, sorted by price |
| GET | `/api/sellers` | Seller directory (id, name, status) used by the mocked identity selector |

Seller endpoints (identity via `X-Seller-Id` request header):

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/seller/listings` | Own listings (all statuses) |
| POST | `/api/seller/listings` | Create listing (APPROVED sellers only) |
| PATCH | `/api/seller/listings/{id}` | Update price/stock/MOQ/status; optional `version` for optimistic locking |
| DELETE | `/api/seller/listings/{id}` | Soft stop: `ACTIVE -> STOPPED` (never a physical delete) |

Status codes: `200`/`204` success, `400` validation, `403` ownership or non-approved seller, `404` missing product/listing/seller, `409` duplicate listing or optimistic-lock conflict.

Admin endpoints (read-only, no identity check — same exposure model as the buyer endpoints; challenge scope):

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/dashboard` | Summary counts: `totalSellers` and `listingsUnderReview` |
| GET | `/api/admin/sellers/count` | Total number of sellers |
| GET | `/api/admin/sellers` | All sellers with id, name and status |
| GET | `/api/admin/sellers/{sellerId}/listings` | Every listing of one seller, all statuses (including STOPPED) |
| GET | `/api/admin/listings/under-review/count` | Listings under review: ACTIVE listings owned by PENDING sellers |

Paged responses serialize as `{ "content": [...], "number", "size", "totalElements", "totalPages", ... }`.

## 10. Database schema

Three tables (see `V1__create_marketplace_schema.sql`):

- `sellers(id, name, status, created_at, updated_at)` — status in `APPROVED|PENDING|REJECTED`
- `products(id, name, description, category, unit, created_at, updated_at)`
- `seller_listings(id, seller_id FK, product_id FK, price, stock_quantity, minimum_order_quantity, status, version, created_at, updated_at)`

Constraints enforced in the database: `UNIQUE(seller_id, product_id)`, `price > 0`, `stock_quantity >= 0`, `minimum_order_quantity > 0`, `status IN ('ACTIVE','STOPPED')`, seller status enum check. Indexes cover product name/category lookups and listing lookups by product/seller/status.

## 11. Product vs Seller Listing

A Product is the catalog entry; a Seller Listing is one seller's offer for that product (price, stock, MOQ, status). Buyers never see "the price of cement" — they see every seller's offer for cement and compare. The `UNIQUE(seller_id, product_id)` constraint means a seller can offer a product at most once.

## 12. Seller isolation approach

There is no production authentication. The current seller is mocked with the `X-Seller-Id` header, read server-side from the request context (never from the request body). Every mutating seller endpoint loads the listing and compares `listing.seller.id` with the header value; mismatches return `403`. Creation additionally requires seller status `APPROVED`.

## 13. Validation rules

Applied in three layers:

1. Bean Validation on request DTOs (`price >= 0.01`, `stock >= 0`, `moq >= 1`) -> `400`
2. Business rules in the service (ownership, approved seller, duplicate listing) -> `403`/`409`
3. Database constraints (checks + unique index) as the last line of defense

The frontend mirrors the numeric rules for immediate feedback but never replaces server-side validation.

## 14. Concurrency strategy

`SellerListing.version` is a JPA `@Version` column (optimistic locking). Concurrent writes to the same row fail with `ObjectOptimisticLockingFailureException`, mapped to `409`. Clients may also send the `version` they last saw on `PATCH`; a mismatch is rejected with `409` before any write, so a stale form can never silently overwrite a newer update.

## 15. Pagination / search approach

All list endpoints are database-paged via Spring Data `Pageable` (`page`, `size`, `sort`). Product search uses `lower(name) LIKE` and an exact category match inside the SQL query — filtering happens in the database, never by loading everything into the application or the browser. Ordering is deterministic (`name, id` for products; `price, id` for offers). For the hypothetical 10M-listing scale, keyset/cursor pagination would replace offset paging on hot endpoints; offset paging is adequate here and keeps the API simple.

## 16. Sample data

`V2__seed_marketplace_data.sql` seeds 4 sellers (2 APPROVED, 1 PENDING, 1 REJECTED), 5 products and 9 listings demonstrating: multiple sellers per product with different price/stock/MOQ, a STOPPED listing, a zero-stock listing, and listings owned by PENDING/REJECTED sellers.

## 17. Business rules (availability)

Buyer-facing offer lists contain only listings that are `ACTIVE` **and** owned by `APPROVED` sellers; everything else is filtered in the query. The frontend derives a single availability label from listing data (one shared function, `frontend/src/availability.js`):

| State | Rule |
|-------|------|
| `STOPPED` | listing status is STOPPED (never purchasable) |
| `SELLER_UNAVAILABLE` | seller status is not APPROVED |
| `OUT_OF_STOCK` | stock = 0 |
| `BELOW_MOQ` | 0 < stock < MOQ (cannot fulfil the minimum order) |
| `LOW_STOCK` | stock < 5 x MOQ |
| `READY` | otherwise |

"Under review" (admin view) uses the existing enums without adding state: a listing is under review while it is `ACTIVE` but its seller is still `PENDING` approval — i.e. it exists but is not published to buyers. `REJECTED` sellers' listings are not under review; they are rejected.

Product cards show `lowestPrice` and `activeSellerCount`, computed by one grouped aggregate query over active/approved listings.

## 18. Assumptions

- Local PostgreSQL with a known development password (not documented here).
- The mocked `X-Seller-Id` identity is acceptable for this challenge; the frontend picks it from a dropdown backed by `GET /api/sellers` and remembers it in `localStorage`.
- The seller dashboard's "add listing" product picker loads the first page (100) of products; at challenge scale this would become a searchable picker.

## 19. Known omissions

- No payments, checkout, RFQ, email/SMS, real authentication, mobile app, microservices, caching layer or search engine — all explicitly out of scope.
- No frontend unit tests; the backend integration suite covers the API contract end to end.
- Offset pagination only (see scaling notes).

## 20. Scaling considerations & production changes

At 1M products / 100K sellers / 10M listings:

- Keep DB-side filtering; add trigram/`pg_trgm` or a dedicated search index for fuzzy name search instead of `LIKE '%…%'`.
- Replace offset pagination with keyset pagination on `(name, id)` / `(price, id)`.
- The offer-summary aggregate is already a single grouped query per page; at scale it could be materialized into a per-product summary table updated on listing writes.
- Add connection pooling (Hikari is default), read replicas for buyer traffic, and caching for hot product pages.
- Production changes: real authentication/authorization (replace `X-Seller-Id`), secrets in environment variables or a vault, HTTPS, structured logging, monitoring, CI/CD, and Flyway migrations reviewed per environment.
