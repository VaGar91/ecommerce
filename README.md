# E-commerce Code Challenge

An enterprise-grade e-commerce application built as a modular monolith. It
delivers product management, product search, validated CSV import, a persistent
shopping cart, and transactional checkout with a fake payment provider.

## Delivered scope

- PostgreSQL local database managed with versioned Flyway migrations.
- Product CRUD, search, pagination, validation, and inventory management.
- Atomic CSV validation and import with idempotent SKU-based upserts.
- Transactional purchase flow with deterministic fake payment outcomes.
- React interfaces for product administration, CSV import, catalog search,
  cart review, and checkout.
- Multi-stage, non-root application containers orchestrated with Docker
  Compose behind a single local HTTP entry point.
- Unit, module-boundary, persistence integration, API integration, and frontend
  component tests.

## Technology baseline

- Java 21
- Spring Boot 4.1
- Spring Modulith 2.1
- PostgreSQL and Flyway
- Maven Wrapper
- Testcontainers
- React 19, TypeScript, and Vite
- NGINX and Docker Compose

The frontend is a separate React application inside this repository, built with
TypeScript and Vite.

## Architecture

The backend is a modular monolith. Spring Modulith treats the direct subpackages
of `com.gila.ecommerce` as application modules:

- `catalog` owns products, inventory, and product search.
- `productimport` parses and coordinates CSV imports through the catalog API.
- `ordering` owns the purchase workflow and order state.
- `payment` defines the payment boundary and its fake implementation.
- `shared` contains technical concerns only, such as configuration and error
  handling. It must not contain business rules.

A business module exposes its API from its root package. Code below an `internal`
package is private to that module. `shared` is intentionally open because it
contains cross-cutting technical utilities. The allowed compile-time dependencies
are:

- `catalog` → `shared`
- `productimport` → `catalog`, `shared`
- `ordering` → `catalog`, `payment`, `shared`
- `payment` → `shared`
- `shared` → no application module

`ModularArchitectureTest` verifies these rules and rejects cycles or access to
another module's internal implementation.

## Development approach

The application was built in independently verifiable vertical slices. The
initial commits established the project, database migrations, and enforceable
module boundaries. Subsequent slices added the catalog, CSV import, ordering,
container packaging, and each frontend workflow. This kept every commit focused
and allowed the domain and API contracts to stabilize before the UI depended on
them.

Business invariants are enforced on the backend even when the frontend performs
the same validation for faster feedback. Prices and stock displayed in the cart
are snapshots for the user experience; checkout reloads and locks the current
database products before charging or changing inventory. Database migrations,
not automatic schema generation, define the production schema.

Tests are placed at the narrowest useful boundary:

- Domain and application unit tests cover catalog rules and CSV parsing.
- Spring Modulith tests enforce module encapsulation and dependency direction.
- Testcontainers integration tests exercise Flyway, PostgreSQL, transactions,
  locking behavior, and HTTP contracts against the real database engine.
- Frontend tests cover routing, forms, search, cart persistence, import reports,
  and successful and failed checkout states.

The final Docker Compose topology is tested as the deployable unit: NGINX serves
the compiled frontend and proxies `/api` to the private Spring Boot service,
which connects to PostgreSQL on the internal Compose network.

## Decisions and alternatives considered

### Modular monolith instead of microservices

A modular monolith preserves explicit catalog, import, ordering, and payment
boundaries while keeping this challenge a single deployable backend. Spring
Modulith makes those boundaries executable through architecture tests and
private `internal` packages.

Independent microservices were rejected because they would introduce network
contracts, service discovery, distributed tracing, deployment coordination,
and partial-failure handling without improving the requested workflows. A
traditional technical-layer structure was also considered, but grouping all
controllers, services, and repositories together would make ownership and
cross-domain dependencies less visible.

### PostgreSQL instead of an in-memory or document database

PostgreSQL provides the transactions, uniqueness constraints, decimal types,
row locks, and relational order history required by catalog and checkout. Using
the same database through Testcontainers in tests avoids differences between
test and runtime SQL behavior.

H2 would make tests faster to start but can hide PostgreSQL-specific migration,
locking, indexing, and SQL behavior. MongoDB would support flexible product
documents, but the current model benefits more from relational constraints and
transactional stock/order changes. SQLite was not selected because PostgreSQL
better represents the concurrency and schema-management expectations of an
enterprise Java service.

### React and TypeScript in the same repository

React provides a focused single-page UI for the administration and storefront
workflows, while TypeScript keeps API models and component state explicit. The
frontend lives in the same repository so one commit and one Compose command
describe a compatible full-stack version.

Server-rendered Thymeleaf would reduce the number of build tools, but it would
couple page rendering to the backend and make the cart and interactive search
experience less isolated. ClojureScript was permitted and offers functional
programming strengths, but it would add a less conventional evaluation and
build surface without a domain requirement that benefits from it. Separate
frontend and backend repositories were rejected because coordinated review,
versioning, and local startup would become more complex for this challenge.

### Free-form categories instead of an enum

Categories are validated strings because the supplied data contains an
open-ended set of values. A Java enum would require a deployment for every new
category. A dedicated category aggregate and administration workflow would be
appropriate if categories needed identities, hierarchy, merchandising rules,
or lifecycle management, but those requirements are outside this scope.

### PostgreSQL search instead of a separate search service

Search uses case-insensitive database predicates with pagination. PostgreSQL
trigram GIN indexes support contains queries, and a functional index supports
the exact category filter. This keeps search transactionally consistent with
product changes and avoids another runtime dependency.

Elasticsearch or OpenSearch would become attractive for large catalogs,
language-aware ranking, facets, synonyms, and typo tolerance. For the challenge
dataset, they would add synchronization, eventual consistency, and operational
cost without a demonstrated need. The catalog boundary allows that search
implementation to be replaced later without changing the web contract.

### Atomic CSV upsert instead of partial or insert-only import

The importer validates the complete document before writing and rejects the
entire file when any row is invalid. This prevents a user from having to infer
which portion of a failed file was committed. Valid files upsert by normalized
SKU, so retrying the same import is safe and refreshes catalog data rather than
creating duplicates.

Partial row-by-row import was rejected because it creates ambiguous recovery
and reconciliation work. Insert-only behavior was rejected because supplier
feeds commonly contain updates. An asynchronous job would be preferable for
very large files, but the explicit 5 MB and 10,000-row limits keep synchronous
processing bounded and give the UI an immediate result.

### Transactional checkout behind a payment boundary

The fake payment provider is an in-process implementation of a small payment
port. Checkout locks requested products in stable identifier order, validates
stock, records immutable line snapshots, and completes within one database
transaction. This makes every fake decline or catalog conflict roll back cleanly.

A real remote payment call must not be treated this way: it would require an
idempotency key, expiring inventory reservation, `PENDING` order state,
asynchronous confirmation or compensation, and an outbox for reliable state
publication. Those mechanisms were deliberately not simulated because the
challenge explicitly removes the external payment provider.

### Browser cart instead of a server-side cart

The cart is stored in React context and local storage so it survives navigation
and refresh without introducing customer identity or sessions. The server does
not trust that state: product IDs and quantities are revalidated during
checkout, and prices come from the locked database rows.

A server-side cart would be the better choice for authenticated customers,
cross-device continuity, promotions, or abandoned-cart processing. Passing the
cart only through page props was rejected because it would couple persistence
and navigation to the route tree.

### One public HTTP entry point

The production frontend container serves static assets and reverse-proxies
`/api` to the backend. This produces same-origin browser requests without a
permissive CORS configuration and keeps the Spring Boot port private to the
Compose network. The frontend and PostgreSQL host bindings are restricted to
loopback for local evaluation.

Publishing the frontend and API independently would be appropriate when they
are deployed and scaled separately, but it would require explicit origin,
TLS, and CORS configuration. Serving the React build from Spring Boot was also
possible, but the NGINX boundary keeps static delivery independent and lets the
backend image contain only the Java runtime and application.

## Example data

The example CSV file supplied with the challenge was downloaded on
**2026-08-19**.

The supplied document is an adversarial validation example rather than a clean
seed file. It contains valid quoted and Unicode text alongside malformed prices,
negative stock, missing required values, zero weight, a duplicate SKU, and
HTML- and SQL-like strings. As supplied, it is expected to return `422
Unprocessable Content`; the response and import UI report the invalid rows and
the database remains unchanged. The string payloads are treated as data: JPA
uses parameterized persistence and React escapes rendered text.

To exercise the successful path, correct or remove every invalid row and keep
each normalized SKU unique within the file. A corrected file is imported in one
transaction, and importing it again updates the existing products by SKU.

## Running with Docker

Prerequisite: Docker with Docker Compose v2.

Build the application image and start the complete stack:

```shell
docker compose up --build --detach --wait
```

Open the application at `http://localhost:8080`. The frontend health endpoint is
`http://localhost:8080/healthz`, and the proxied backend health endpoint is
`http://localhost:8080/api/actuator/health`. `APP_PORT` changes the application
host port, while `POSTGRES_PORT` changes the PostgreSQL host port. The services
still communicate on their fixed container ports.

Only the frontend reverse proxy is published as the application entry point.
The Spring Boot service remains private to the Compose network, and the browser
reaches it through same-origin `/api` requests. Both the frontend and PostgreSQL
host ports are bound to `127.0.0.1`, so the local development stack is not
published on external network interfaces.

Inspect service state and logs:

```shell
docker compose ps
docker compose logs frontend
docker compose logs app
```

Stop the stack without deleting PostgreSQL data:

```shell
docker compose down
```

Use `docker compose down --volumes` only when the local database data should
also be deleted.

Both application images are built in two stages. Maven and the JDK remain in the
backend builder stage; its final image contains only the Java 21 runtime, runs
as the unprivileged numeric user `10001`, and defines its own actuator health
check. Node and frontend build dependencies remain in the frontend builder
stage; its final image contains only the compiled static assets and an
unprivileged NGINX reverse proxy. NGINX provides client-side route fallback,
security headers, and the single `/api` gateway to Spring Boot.

## Running the backend from source

Prerequisites:

- JDK 21
- Docker with Docker Compose v2

Start PostgreSQL:

```shell
docker compose up -d postgres
```

Start the backend in another terminal:

```shell
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`. Its health endpoint is
available at `http://localhost:8080/actuator/health`.

The local defaults can be overridden with `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`. `POSTGRES_PORT` changes the host port exposed by Compose; when it
is changed, `DB_URL` must point to the same port. The committed credentials are
for local development only. PostgreSQL is exposed on host port `15432` by default
to avoid conflicting with an existing installation on the conventional `5432`
port, and is bound to the loopback interface only.

Stop PostgreSQL without deleting its data:

```shell
docker compose down
```

## Running the frontend from source

Prerequisites:

- Node.js 24.15 or newer
- The backend running on `http://localhost:8080`

Install dependencies and start Vite:

```shell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` requests to the backend, so
the browser uses a same-origin API path and no development CORS configuration is
required.

Run all frontend checks with:

```shell
cd frontend
npm run check
```

## Running tests

```shell
./mvnw test
```

The integration test suite uses Testcontainers and therefore requires Docker.
It starts an isolated PostgreSQL instance and does not use the Compose database.

## Product API

Product CRUD is available below `/api/products`:

- `POST /api/products` creates a product and returns `201 Created`.
- `GET /api/products/{id}` returns one product.
- `GET /api/products` searches products and returns a page ordered by name and
  SKU.
- `PUT /api/products/{id}` replaces a product.
- `DELETE /api/products/{id}` deletes a product and returns `204 No Content`.

Create and update requests use this shape:

```json
{
  "name": "Mechanical Keyboard",
  "sku": "KEY-001",
  "description": "Tactile mechanical keyboard",
  "category": "Accessories",
  "price": 49.90,
  "stock": 25,
  "weightKg": 0.850
}
```

SKUs are trimmed, normalized to uppercase, and must be unique. Invalid requests
use `application/problem+json` responses with field-level validation errors.

Product search accepts these optional query parameters:

- `query` performs a case-insensitive contains search across name, SKU,
  description, and category.
- `category` applies a case-insensitive exact category filter.
- `page` selects a zero-based page and defaults to `0`.
- `size` controls the page size, defaults to `20`, and must be between `1` and
  `100`.

## Product CSV import

Upload a CSV as the `file` part of a multipart request:

```shell
curl --fail-with-body \
  --form "file=@products.csv;type=text/csv" \
  http://localhost:8080/api/product-imports
```

The file must be UTF-8, no larger than 5 MB, and contain at most 10,000 product
rows. Its header must contain exactly these columns; their order may vary:

```text
name,sku,description,category,price,stock,weight_kg
```

Completely blank lines are ignored. Every nonblank row is validated before any
database write. If a row is invalid or a SKU appears twice in the file, the API
returns `422 Unprocessable Content` with row and field errors, and the entire
import is rejected. A valid file is committed atomically and upserts products by
normalized SKU, making repeated imports idempotent. The response reports total,
created, and updated row counts.

The supplied example CSV intentionally exercises validation cases, including
nonnumeric prices, negative stock, missing required fields, zero weight, and
duplicate SKUs.

## Purchase API

Create and pay for an order with `POST /api/orders`:

```json
{
  "items": [
    {
      "productId": "2e7fbff8-cdf6-4f9f-aee8-997434940848",
      "quantity": 2
    }
  ],
  "paymentToken": "tok_approved"
}
```

The fake payment gateway approves any nonblank token except `tok_declined`,
which deterministically simulates a provider decline. A successful purchase
returns `201 Created` with a `Location` header and an immutable order snapshot.
The order can subsequently be read with `GET /api/orders/{orderId}`.

Checkout locks all requested products in a stable order, validates inventory,
decrements stock, charges the fake provider, and persists the paid order in one
database transaction. A missing product returns `404`, insufficient stock
returns `409`, and a fake payment decline returns `402`; each failure rolls back
all inventory changes. Order lines retain product name, SKU, and unit price as
they were at purchase time so later catalog changes do not rewrite order
history.

Keeping checkout in one transaction is appropriate for this challenge because
the payment provider is an in-process fake with no independent side effects. A
real remote provider would instead use an idempotency key, an expiring inventory
reservation, and an orchestrated `PENDING` → `PAID`/`FAILED` workflow backed by
an outbox. That avoids holding database locks during network calls and handles
the case where payment succeeds but the local transaction cannot commit.

## Frontend cart state

The storefront keeps the cart in a React context and persists it to browser
local storage so a refresh does not discard an in-progress order. Stored names,
prices, and stock are display snapshots only. Checkout sends product IDs and
quantities to the backend, which reloads the products under database locks and
revalidates current prices and availability. Client state is therefore useful
for the shopping experience but never trusted as a purchase authority.

Server-side carts were considered, but would add customer identity, session
management, and abandoned-cart lifecycle concerns that are outside this
challenge. Passing the cart through page-level props was also rejected because
navigation and persistence would become tightly coupled to the route tree.

## Assumptions and deliberate boundaries

- The application is a local evaluation environment, so it does not implement
  user accounts, authentication, or administrator authorization.
- Product prices use a single implicit currency. Currency conversion, taxes,
  discounts, shipping, and refunds are outside the requested purchase flow.
- Product categories are flat text labels; category hierarchy and management
  are not part of the challenge.
- The fake gateway has no independent side effects and recognizes
  `tok_declined` as its deterministic failure case. No real payment credentials
  are needed or accepted.
- The local cart belongs to one browser profile. The backend remains authoritative
  for product existence, price, and stock at checkout.
- CSV import is synchronous within documented size and row limits. Larger feeds
  would be processed as background jobs with durable progress reporting.
- Committed database credentials are local-only defaults. A real deployment
  would inject secrets, terminate TLS, define backup/restore procedures, and
  restrict administrative endpoints.
- Actuator health endpoints provide container readiness information. Centralized
  logs, metrics, tracing, alerting, and rate limiting would be deployment-level
  additions rather than simulated challenge features.

## Project status

The backend bootstrap, module boundaries, local database infrastructure,
product CRUD and search, atomic CSV import, and transactional checkout with a
fake payment provider are complete. The React UI includes product administration,
CSV import, storefront search, a persisted cart, and transactional checkout.
The complete frontend, backend, and PostgreSQL stack is containerized behind a
single local entry point. All functionality explicitly requested by the
challenge is implemented and can be evaluated with the Docker instructions
above. Continuous integration, browser automation, and production platform
integration are optional extensions, not prerequisites for running the
submission locally.
