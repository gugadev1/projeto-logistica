# Logistic Project

A portfolio project designed to solve a real operational problem: planning and simulating last-mile delivery with business constraints such as weight, volume, capacity, and route feasibility.

This repository is intentionally focused on software engineering quality, domain modeling, and optimization-oriented thinking.


## Run Via CLI

Prerequisites:

- Java 21+
- Maven 3.9+

Commands:

```bash
mvn clean compile
mvn exec:java@run-cli
```

The CLI will ask for vehicle data, origin coordinates, and package data. It then:

- validates weight and volume constraints,
- rejects invalid loads with clear reasons,
- and prints dispatch order using express-first priority and Euclidean distance.

## Run Web Dashboard

Start the web server:

```bash
mvn clean compile
mvn exec:java@run-web
```

Open in browser:

- `http://localhost:8080`

Dashboard features:

- vehicle occupancy bar with color feedback (green/yellow/red),
- dispatch manifest table in the exact order produced by the routing algorithm,
- styled browser alert when capacity exception is triggered by Java backend rules.

## Persistence and Traceability (Phase 5)

This project now persists operational history in PostgreSQL with versioned migrations.

### What is persisted

- `veiculo`: master vehicle records.
- `operacao_execucao`: execution audit trail with status, timestamps, request payload, and error message.
- `simulacao`: simulation header data (origin, occupancy, and final totals).
- `simulacao_pacote`: package-level records for each simulation.
- `romaneio_item`: final dispatch sequence used in the output manifest.

### Migration strategy

- Flyway is executed automatically on server startup.
- Initial schema migration file: `src/main/resources/db/migration/V1__init_persistencia_logistica.sql`.

### PostgreSQL quick start (Docker)

```bash
docker run --name pg-logistica \
	-e POSTGRES_DB=logistica \
	-e POSTGRES_USER=logistica \
	-e POSTGRES_PASSWORD=logistica \
	-p 5432:5432 \
	-d postgres:17
```

### Environment variables

```bash
export DB_URL=jdbc:postgresql://localhost:5432/logistica
export DB_USER=logistica
export DB_PASSWORD=logistica
export DB_SCHEMA=public
export DB_POOL_SIZE=10
```

### Run web server with persistence

```bash
mvn clean compile
mvn exec:java@run-web
```

During each simulation request, the backend records:

- success flow (`PROCESSANDO` -> `SUCESSO`) in a single transaction,
- validation/capacity/internal failures with final status and error details.