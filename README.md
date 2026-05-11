# Order Manager

A Kotlin/Spring Boot backend that acts as a bridge between external clients (e.g. supermarket distribution centers) and a LIMS (Laboratory Information Management System). It manages the full lifecycle of lab orders: from ingestion to report delivery.

## Domain Model

```mermaid
classDiagram
    direction TB

    class LabOrder {
        +UUID id
        +String clientId
        +String clientOrderId
        +LabOrderStatus status
        +Boolean sent
        +Instant createdAt
        +Instant updatedAt
    }

    class Sample {
        +UUID id
        +String sampleName
    }

    class Analysis {
        +UUID id
        +String name
    }

    class AnalysisResult {
        +UUID id
        +String value
        +String unit
        +String referenceRange
    }

    class Report {
        +UUID id
        +String orderStatus
        +String resultAnalyses
        +Instant createdAt
    }

    class LabOrderStatus {
        <<enumeration>>
        NEW
        VALIDATED
        COMPLETE
        FAILED
    }

    LabOrder "1" --> "0..*" Sample : contains
    Sample "1" --> "0..*" Analysis : contains
    Analysis "1" --> "0..1" AnalysisResult : has
    LabOrder "1" --> "0..1" Report : generates
    LabOrder --> LabOrderStatus
```

An **order** belongs to a client and contains one or more **samples**. Each sample has one or more **analyses** to be performed. Once the LIMS completes the analyses, it creates a **report** and transitions the order to `COMPLETED`.

Order statuses: `NEW` → `VALIDATED` → `IN_PROGRESS` → `COMPLETED`

## Architecture

```
External Clients ──▶ Order Manager ──(RabbitMQ)──▶ LIMS
                          ▲
                          │
                     SOAP API (POST /orders/soap-client)
```

- **REST API** — exposes endpoints for clients and the LIMS
- **RabbitMQ** — orders are published to a queue and consumed by the LIMS
- **SOAP integration** — the SOAP API pushes XML orders to `POST /orders/soap-client`, which uses Kotlin coroutines to transform and publish each order to RabbitMQ in parallel
- **PostgreSQL** — persists orders, samples, analyses, and reports

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/orders` | Create a new order |
| `GET` | `/orders` | List all orders |
| `GET` | `/orders/{clientOrderId}` | Get order by client order ID |
| `POST` | `/orders/{clientOrderId}/status` | Update order status (called by LIMS) |
| `POST` | `/orders/{clientOrderId}/report` | Submit a report (called by LIMS) |
| `GET` | `/orders/{clientOrderId}/report` | Get the report for an order |
| `POST` | `/orders/soap-client` | Receive XML orders from the SOAP API |

## Tech Stack

| Technology | Version |
|------------|---------|
| Kotlin | 2.2.21 |
| Spring Boot | 4.0.6 |
| PostgreSQL | 16 |
| RabbitMQ | 3 |
| Kotlin Coroutines | — |
| Maven | — |

## Database Schema

```mermaid
erDiagram
    orders {
        uuid id PK
        string client_id
        string client_order_id UK
        string status
        boolean sent
        timestamp created_at
        timestamp updated_at
    }

    samples {
        uuid id PK
        string sample_name
        uuid order_id FK
    }

    analyses {
        uuid id PK
        string name
        uuid sample_id FK
    }

    analysis_results {
        uuid id PK
        string value
        string unit
        string reference_range
        uuid analysis_id FK
    }

    reports {
        uuid id PK
        uuid order_id FK
        string order_status
        jsonb result_analyses
        timestamp created_at
    }

    orders ||--o{ samples : "has"
    samples ||--o{ analyses : "has"
    analyses ||--o| analysis_results : "has"
    orders ||--o| reports : "generates"
```

## Running Locally

Start the required infrastructure:

```bash
docker compose -f docker-compose-test.yml up -d
```

Then run the application:

```bash
./mvnw spring-boot:run
```

## Running Tests

Integration tests require the Docker Compose containers to be running (see above).

```bash
./mvnw test
```

Unit tests use [MockK](https://mockk.io/) and [kotlinx-coroutines-test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/) and run without any infrastructure.

## Example Payloads

See the [`documentation/`](documentation/) folder for example JSON and XML payloads.
