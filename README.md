# E-commerce Code Challenge

An enterprise-grade e-commerce application built as a modular monolith. The
planned scope includes product management, product search, CSV import, and a
checkout flow with a fake payment provider.

## Technology baseline

- Java 21
- Spring Boot 4.1
- Spring Modulith 2.1
- PostgreSQL and Flyway
- Maven Wrapper
- Testcontainers
- React 19, TypeScript, and Vite

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

## Example data

The example CSV file supplied with the challenge was downloaded on
**2026-08-19**.

## Running with Docker

Prerequisite: Docker with Docker Compose v2.

Build the application image and start the complete stack:

```shell
docker compose up --build --detach --wait
```

The API is available at `http://localhost:8080`, and its health endpoint is
`http://localhost:8080/actuator/health`. `APP_PORT` changes the application host
port, while `POSTGRES_PORT` changes the PostgreSQL host port. The services still
communicate on their fixed container ports. PostgreSQL is bound to `127.0.0.1`
and is therefore reachable from the local machine but not published on external
network interfaces.

Inspect service state and logs:

```shell
docker compose ps
docker compose logs app
```

Stop the stack without deleting PostgreSQL data:

```shell
docker compose down
```

Use `docker compose down --volumes` only when the local database data should
also be deleted.

The application image is built in two stages. Maven and the JDK remain in the
builder stage; the final image contains only the Java 21 runtime, runs as the
unprivileged numeric user `10001`, and defines its own actuator health check.

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

## Current status

The backend bootstrap, module boundaries, local database infrastructure,
product CRUD and search, atomic CSV import, and transactional checkout with a
fake payment provider are in place. The backend is packaged as a production-style
container and orchestrated locally with PostgreSQL. The React frontend foundation
plus the product administration, atomic CSV import, and storefront search UIs
are in place. The persisted cart foundation is ready; the cart review and
checkout screens remain.
