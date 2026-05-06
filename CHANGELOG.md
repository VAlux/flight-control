# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased] — 2026-05-07

### Added

- **REST API** (`/api/v1/flights`) — six endpoints: list with optional origin/destination/status filters and pagination, create (201 + `Location` header), get by ID, update, delete, and PATCH status transition.
- **Flight state machine** — enforces allowed status transitions (SCHEDULED → DELAYED/DEPARTED/CANCELLED, DELAYED → DEPARTED/CANCELLED, DEPARTED → IN_AIR, IN_AIR → LANDED; LANDED and CANCELLED are terminal).
- **Eight business rules** enforced server-side: departure time must be in the future, arrival after departure, origin ≠ destination, unique flight number on create, unique flight number on update (excluding self), only SCHEDULED/DELAYED flights may be updated, only SCHEDULED flights may be deleted, transitions must follow the state machine.
- **RFC 9457 Problem Detail error responses** — all errors return `application/problem+json` with `type`, `title`, `status`, `detail`, `instance`, `timestamp`, `correlation_id`, and a field-level `errors` array for validation failures.
- **`X-Request-ID` correlation header** — generated or echoed on every response (success and error) and stored in MDC for structured logging.
- **H2 in-memory persistence** — `schema.sql`-managed `flight` table; `OffsetDateTime` stored and retrieved correctly via an `AttributeConverter`.
- **OpenAPI / Swagger UI** — served at `/swagger-ui.html` via springdoc-openapi.
- **Actuator health check** — `GET /actuator/health`.
- **Vanilla JS frontend** served as static files (no build step):
  - `index.html` — filterable, paginated flight table with create-flight modal, delete confirmation, and status badges.
  - `flight-detail.html` — full detail view with status transition buttons (only valid next states shown), inline edit form (SCHEDULED/DELAYED only), and delete action (SCHEDULED only).
  - Toast notifications (success/error), loading spinner, and confirmation dialog shared across both pages.
  - Cross-page delete toast delivered via `sessionStorage`.
  - Timezone-correct `datetime-local` ↔ ISO 8601 conversion in the edit form.
- **Test suite — 134 tests, 0 failures**:
  - 44 parameterized state-machine tests covering all 36 transition combinations.
  - 12 `@DataJpaTest` repository tests (save, filter, uniqueness checks, Specification-based pagination).
  - 9 mapper unit tests (round-trip fidelity, Bean Validation, null guards).
  - 34 `@ExtendWith(MockitoExtension.class)` service tests (all 8 business rules × happy + failure paths).
  - 22 `@WebMvcTest` controller tests (all endpoints, RFC 9457 error shapes, `X-Request-ID` echo, page/size constraint enforcement).
  - 12 `@SpringBootTest` integration tests (end-to-end lifecycle against real H2).
  - 1 smoke test (context loads).

### Changed

- _Nothing changed — this is the initial implementation._

### Fixed

- _Nothing fixed — this is the initial implementation._

### Removed

- _Nothing removed — this is the initial implementation._
