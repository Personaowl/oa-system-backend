# Architecture baseline

## Request path

1. The frontend calls `/api/v1/**` through `oa-gateway`.
2. Gateway removes untrusted identity headers, assigns `X-Trace-Id`, and validates JWT except for public paths.
3. Gateway discovers downstream services through Nacos and injects trusted user context headers.
4. Each service owns its business rules and tables. Cross-service calls use explicit HTTP contracts, never shared entities or mappers.
5. API responses use `ApiResponse<T>` and include `code`, `message`, `data`, `traceId`, and `timestamp`.

## Ownership rules

- `oa-common-*` contains stable cross-service infrastructure only.
- Gateway is reactive and must not depend on servlet-based `oa-common-web`.
- A service cannot reference another service's entity or mapper classes.
- Writes are not automatically retried across services.
- Elasticsearch and AI indexing are secondary operations; failures must not roll back primary business data.

## Configuration

Services import `oa-common.yaml` and their own `<application-name>.yaml` from Nacos through `spring.config.import`.

Secrets must be supplied by environment variables or protected configuration. Never commit JWT secrets, database passwords, GitHub tokens, or model API keys.

