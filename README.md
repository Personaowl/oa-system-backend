# OA System Backend

Spring Boot 3.5 / Spring Cloud Alibaba microservice backend for the JavaEE course project.

## Modules

| Module | Port | Responsibility |
| --- | ---: | --- |
| `oa-gateway` | 8080 | Routing, trace ID, JWT authentication |
| `oa-user-service` | 8101 | Login, organization, employee and RBAC |
| `oa-attendance-service` | 8102 | Check-in/out and attendance records |
| `oa-flow-service` | 8103 | Leave/overtime requests and approval tasks |
| `oa-notice-service` | 8104 | Notices and read state |
| `oa-ai-service` | 8105 | Spring AI, Ollama and Redis vector search |
| `oa-document-service` | 8106 | Department workspaces and shared rich-text documents |

Shared libraries are under `oa-common`. Business modules may depend on common modules; common modules must never depend on business modules.

## Prerequisites

- JDK 21
- Maven 3.9+
- MySQL 8
- Nacos 3.2.2 (`127.0.0.1:8848`，gRPC `9848/9849`)
- Redis 8 (`127.0.0.1:6379`)
- Elasticsearch 8.18.1 (`127.0.0.1:9200`)
- Ollama (`127.0.0.1:12434`) for the AI service

The infrastructure Compose file lives at `D:\oa-system\ops\compose.yaml` in the team workspace.

## Build

```powershell
mvn clean verify
```

To build one service and its dependencies:

```powershell
mvn -pl oa-user-service -am clean verify
```

## Run locally

1. Copy `.env.example` values into your IDE environment or terminal.
2. Create the database with `sql/00-create-database.sql`.
3. Start infrastructure and verify Nacos, Redis and Elasticsearch.
4. Start the services, then start the gateway last.

Example:

```powershell
mvn -pl oa-user-service spring-boot:run
```

All Nacos imports are optional so a module can still be compiled and unit-tested without Nacos. Business database calls require valid local database credentials.

## Authentication integration checkpoint

The login → JWT → Gateway authentication → current-user flow is implemented. Run `sql/02-seed-local-admin.sql`, then see [docs/auth-api.md](docs/auth-api.md) for the local test account and requests.

## Attendance service checkpoint

The attendance module implements check-in, today status, check-out, paged records, personal monthly statistics, and authorized administrative summaries. Its OpenAPI JSON is available at `/api/v1/attendance/openapi`; the legacy `/api/v1/attendance/status` endpoint is deprecated in favor of `/actuator/health`.

Run the SQL files in numeric order. Re-run `sql/02-seed-local-admin.sql` after pulling attendance changes so the local administrator receives `attendance:record:query` and `attendance:statistics:query`.

See [oa-attendance-service/README.md](oa-attendance-service/README.md) for local configuration, endpoint contracts, rule configuration, tests, and the Gateway end-to-end script.

See [docs/architecture.md](docs/architecture.md) and [docs/contributing.md](docs/contributing.md).

Approval API and database upgrade instructions: [docs/flow-api.md](docs/flow-api.md).
