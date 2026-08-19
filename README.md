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

## Current status

The backend bootstrap, module boundaries, and local database infrastructure are
in place. Business capabilities will be added as independently tested vertical
slices.
