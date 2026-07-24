# OA Attendance Service

`oa-attendance-service` owns attendance records, rule evaluation, concurrency control, record queries, and statistics. Employee identity and permissions come only from trusted headers written by `oa-gateway`.

## Local setup

Run these SQL files in numeric order:

1. `sql/00-create-database.sql`
2. `sql/01-initial-schema.sql`
3. `sql/02-seed-local-admin.sql`
4. `sql/03-attendance-migration.sql`
5. `sql/05-attendance-rule.sql`

`02-seed-local-admin.sql` is idempotent and may be run again after the migration. It grants the local admin the record-query and statistics-query permissions used by this service.

For IntelliJ local execution, set:

```text
SPRING_PROFILES_ACTIVE=local
MYSQL_DATABASE=oa_system
MYSQL_USERNAME=root
MYSQL_PASSWORD=<your-local-password>
```

The local profile disables Nacos discovery/config imports for direct service testing. MySQL and Redis must be running.

## Rule configuration

The canonical Nacos Data ID is `oa-attendance-service.yaml` in group `DEFAULT_GROUP`:

| Property | Default | Purpose |
| --- | --- | --- |
| `oa.attendance.work-start` | `09:00` | Work start time |
| `oa.attendance.work-end` | `18:00` | Work end time |
| `oa.attendance.late-threshold-minutes` | `5` | Inclusive grace period |
| `oa.attendance.lock-ttl-seconds` | `10` | Redis attendance-lock TTL |
| `oa.attendance.zone-id` | `Asia/Shanghai` | Business clock zone |
| `oa.attendance.redis-lock-enabled` | `true` | Redis lock switch; DB constraints remain active |

Keep this table synchronized with `deploy/nacos/oa-attendance-service.yaml` and `AttendanceProperties`.

The values above are startup fallbacks. The active rule is persisted in the single-row
`attendance_rule` table and can be changed by an administrator through the rule API.
Existing attendance records retain the rule snapshot captured at check-in time.

## HTTP API

| Method and path | Purpose | Permission |
| --- | --- | --- |
| `POST /api/v1/attendance/check-in` | Check in current employee | Authenticated |
| `POST /api/v1/attendance/check-out` | Check out current employee | Authenticated |
| `GET /api/v1/attendance/records/export` | Export filtered attendance records and work duration to Excel | Same scope as record query |
| `GET /api/v1/attendance/today` | Current employee today status | Authenticated |
| `GET /api/v1/attendance/records` | Personal or scoped records with employee/department data | Cross-user: `attendance:record:query` |
| `GET /api/v1/attendance/scope` | Visible departments and employee filter options | Authenticated; server-side data scope |
| `GET /api/v1/attendance/statistics/monthly` | Personal monthly statistics | Authenticated |
| `GET /api/v1/attendance/statistics/summary` | Admin/HR all-data or manager department summary | `attendance:statistics:query` |
| `GET /api/v1/attendance/rules/current` | Read active attendance rule | Authenticated |
| `PUT /api/v1/attendance/rules/current` | Update active attendance rule | `attendance:rule:update` |
| `GET /api/v1/attendance/openapi` | OpenAPI 3 JSON | Gateway-authenticated or direct development access |

`GET /api/v1/attendance/status` remains temporarily for compatibility and is deprecated. Use `GET /actuator/health` for service health.

`departmentId` is reserved in the current release. It is never applied as a filter until a versioned user-service data-scope contract exists; responses explicitly report `departmentFilterApplied=false`.

## Verification

Run the module and all required shared modules:

```bash
mvn -pl oa-attendance-service -am -B -ntp verify
```

For the full authenticated path, start MySQL, Redis, Nacos, user-service, attendance-service, then gateway. Re-run the local admin seed and execute:

```bash
OA_PASSWORD='<local-admin-password>' scripts/attendance-e2e.sh
```

Alternatively provide an existing administrator token:

```bash
OA_TOKEN='<access-token>' scripts/attendance-e2e.sh
```

The script accepts `BASE_URL` (default `http://127.0.0.1:8080`) and safely supports repeated runs: duplicate check-in/check-out business codes are treated as valid idempotency evidence.
