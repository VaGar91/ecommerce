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

The frontend will be developed separately with React, TypeScript, and Vite.

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

## Running locally

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
port.

Stop the database without deleting its data:

```shell
docker compose down
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

## Current status

The backend bootstrap, module boundaries, local database infrastructure,
product CRUD and search, and atomic CSV import are in place. Checkout and fake
payment remain and will be added as independently tested vertical slices.
