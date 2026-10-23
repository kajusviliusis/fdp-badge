# BookWorm API

Spring Boot 3 / Java 21 backend for the BookWorm bookstore (see `docs/openapi.yaml`, `docs/data-model.md`).

## Running locally

Requires a PostgreSQL database. The schema is created and versioned by Flyway migrations
(`src/main/resources/db/migration`); Hibernate only validates it (`JPA_DDL_AUTO=validate`).

| Variable       | Purpose                                          | Default                                  |
|----------------|--------------------------------------------------|------------------------------------------|
| `DB_URL`       | JDBC URL                                         | `jdbc:postgresql://localhost:5432/bookworm` |
| `DB_USERNAME`  | Database user                                    | `bookworm`                               |
| `DB_PASSWORD`  | Database password (required)                     | -                                        |
| `JWT_SECRET`   | HS256 signing secret, at least 32 characters (required) | -                                 |

```bash
export DB_PASSWORD=...   # never commit credentials
export JWT_SECRET=...
mvn spring-boot:run      # listens on 127.0.0.1:8080/api/v1
```

## Tests

`mvn verify` runs all tests. Unit and slice tests use H2; `@PostgresDataJpaTest` tests run the Flyway migrations on a
real, embedded PostgreSQL server (no Docker required; binaries are downloaded from Maven Central on first run).
