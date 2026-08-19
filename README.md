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

## Current status

The project bootstrap is in place. Architecture decisions, local infrastructure,
and complete run instructions will be documented as those capabilities are added
in subsequent commits.
