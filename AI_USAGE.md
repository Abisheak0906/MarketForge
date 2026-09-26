# AI Usage

## Tools used

- **Qoder coding agent** (agentic AI assistant in the editor) — used for repository inspection, backend modifications, the full React frontend, test authoring, debugging, and documentation drafting.
- No other AI code-generation tools were used for this submission.

## What the AI was used for

1. **Repository inspection and gap analysis** — reading every backend file, comparing the implementation against the challenge brief, and listing concrete gaps (missing test profile on one test class, no DB-side search/filter, buyer endpoints leaking non-approved sellers' listings, missing seller/product names in listing responses).
2. **Backend changes** — adding `search`/`category` parameters with a database-side query, an offer-summary aggregate (lowest price, active seller count), seller-status filtering on buyer endpoints, optional `version` on listing updates for controlled 409 conflicts, and a seller directory endpoint for the mocked identity selector.
3. **Test authoring** — rewriting the integration test JSON helpers and adding tests for search/filter, pagination, buyer visibility rules, validation, ownership, duplicates, and stale-version conflicts.
4. **Frontend** — generating the Vite + React application (buyer marketplace, product detail, seller dashboard), including forms, validation, loading/empty/error states and the mocked seller selector.
5. **Admin portal** — adding `/api/admin` dashboard summary, sellers/count, sellers list, per-seller listings and under-review listing count by reusing the existing repositories, service, DTOs and enums (under review = ACTIVE listing of a PENDING seller), plus the `/admin` React page (summary cards, seller table, selected-seller listings) and integration tests.
6. **Debugging** — diagnosing build and test failures (see corrections below).
7. **Documentation** — drafting this file and README.md from the verified behaviour of the code.

## What was manually reviewed or verified by the developer/agent loop with real commands

- Every backend and frontend change was compiled and executed: `mvnw clean test` (19/19 green), `npm run build`, live backend smoke tests with curl against all documented status codes, and in-browser verification of every frontend flow (browse, search, filter, detail, edit, stop/resume, add listing, validation errors, PENDING-seller banner).
- Database state was inspected with `psql` after each phase to prove test isolation (`bajrix_test` holds test fixtures, `bajrix` holds only seed data).
- The schema (V1) and seed data (V2) were kept as-is; no business logic was rewritten to satisfy test-environment problems.

## Concrete examples where an AI suggestion was changed, rejected or corrected

1. **In-memory filtering rejected.** The agent's first draft of `getProduct` fetched the whole paged product list and filtered it in Java to attach the offer summary — exactly the anti-pattern the challenge forbids. It was rejected and replaced with a single-row aggregate query (`findWithOfferSummaryById`, later folded into `summarizeOffersForProducts`).
2. **Wrong Jackson package corrected.** The generated test used `com.fasterxml.jackson.databind.ObjectMapper` (the pre-2025 default). Compilation failed because Spring Boot 4 ships Jackson 3; the import was corrected to `tools.jackson.databind.ObjectMapper` after reading the compiler error.
3. **Tuple JPQL query replaced after runtime failure.** A `SELECT p, (subquery), (subquery)` projection failed at runtime (`lower(bytea)` from an untyped null parameter, and unstable `Object[]` unwrapping for `Optional` results). The approach was abandoned in favour of a paged product query plus one grouped aggregate query merged in the service.
4. **Page JSON shape corrected from evidence.** Tests initially asserted Spring Boot 3.x-style `$.page.totalElements`; the actual Boot 4 response puts `totalElements` at the top level. The assertion (and the frontend contract) were fixed after printing the real response.
5. **"Remove all comments" instruction bounded by Flyway reality.** The user asked for zero comment lines. Comments were stripped everywhere except the already-applied Flyway migrations; when they were finally stripped from V2 as well, both local databases were dropped and re-migrated from scratch so Flyway checksums stayed consistent, instead of silently leaving a broken migration history.

## Statements not made

No AI tool wrote this document automatically from guesses: every claim above corresponds to a command that was actually run in this workspace.
