# Progress Log

## 2026-05-07 — Planning Complete

9 increments defined. No code written yet.

### Increment Status

| # | Title | Status |
|---|---|---|
| 1 | Project Scaffold & Build | completed |
| 2 | Domain Model & Persistence Layer | completed |
| 3 | State Machine (Pure Domain) | completed |
| 4 | DTOs & Mapping | completed |
| 5 | Service Layer & Business Rules | completed |
| 6 | REST Controller & Error Handling | completed |
| 7 | Integration Test | pending |
| 8 | Frontend: Flight List Page | pending |
| 9 | Frontend: Flight Detail Page | pending |

---

## 2026-05-07 — Increment 1: Project Scaffold & Build

- **What was completed:** Working Spring Boot 3.3.5 / Java 17 application scaffold. `./mvnw verify` passes (1 test, 0 failures). Application context loads, H2 in-memory datasource initialises, `/actuator/health` endpoint available, Swagger UI configured at `/swagger-ui.html`.
- **Interfaces/methods created:** None (scaffold only — `FlightControlApplication.main` entry point).
- **Files created/modified:**
  - `pom.xml` — Spring Boot 3.3.5 parent; deps: web, data-jpa, validation, actuator, springdoc-openapi-starter-webmvc-ui 2.5.0, h2 (runtime), lombok, spring-boot-starter-test
  - `src/main/java/com/flightcontrol/FlightControlApplication.java` — `@SpringBootApplication` main class
  - `src/main/resources/application.properties` — H2 in-memory datasource, `ddl-auto=none`, `sql.init.mode=always`, `defer-datasource-initialization=true`, Jackson UTC config, springdoc swagger-ui path
  - `src/test/java/com/flightcontrol/FlightControlApplicationTests.java` — `@SpringBootTest` context-loads smoke test
  - `mvnw` — Maven wrapper shell script (downloads Maven 3.9.6 on first run)
  - `mvnw.cmd` — Maven wrapper batch script for Windows
  - `.mvn/wrapper/maven-wrapper.properties` — wrapper config pointing to Maven 3.9.6 distribution
- **Decisions made:**
  - Used Spring Boot 3.3.5 (latest 3.3.x patch at time of implementation).
  - springdoc version pinned to 2.5.0 (latest 2.5.x compatible with Spring Boot 3.3.x).
  - H2 datasource URL includes `DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` to keep the in-memory DB alive across connections during tests.
  - H2Dialect explicitly set in properties (produces a deprecation warning from Hibernate — harmless, will be cleaned up when Hibernate auto-detection is confirmed sufficient in later increments).
  - `spring.jpa.open-in-view` not explicitly set yet — warning noted, will be disabled in a later increment once controllers exist.
  - Maven wrapper downloads Maven 3.9.6 to `~/.m2/wrapper/dists/` on first invocation; subsequent runs use the cached distribution.
- **Tests:** 1 passing, 0 failures (`./mvnw verify` — BUILD SUCCESS in 8 s)

## 2026-05-07 — Test Run (Increment 1, Attempt 1)
- Passed: 1 | Failed: 0 | Skipped: 0
- Build: SUCCESS
- Coverage: Not configured (coverage gate: SKIPPED — no JaCoCo plugin in pom.xml)
- Test Duration: 2.5s
- Overall Duration: 3.8s
- Failures: None
- Action: All clear — Increment 1 complete and verified.


## 2026-05-07 — Code Review (Increment 1)
- Quality: PASS (Critical: 0, High: 0, Medium: 3, Suggestion: 4)
- Coverage: FULLY COVERED
- Recommendation: approve — address 3 medium findings in current or next increment before they accumulate

---

## 2026-05-07 — Increment 2: Domain Model & Persistence Layer

- **What was completed:** `Flight` JPA entity, `FlightStatus` enum, `OffsetDateTimeConverter`, `FlightRepository`, and `schema.sql`. All 11 repository tests plus the Increment 1 smoke test pass (`./mvnw test` — 12 tests, 0 failures, BUILD SUCCESS).
- **Interfaces/methods created:**
  - `FlightStatus` enum — values: `SCHEDULED, DELAYED, DEPARTED, IN_AIR, LANDED, CANCELLED`
  - `Flight` entity — `@Entity @Table(name="flight")`; fields: `id (UUID)`, `flightNumber`, `airline`, `origin`, `destination`, `departureTime`, `arrivalTime`, `status`, `createdAt`, `updatedAt`; lifecycle callbacks `@PrePersist` / `@PreUpdate`
  - `OffsetDateTimeConverter` — `@Converter(autoApply=true)` mapping `OffsetDateTime ↔ java.sql.Timestamp` (UTC)
  - `FlightRepository extends JpaRepository<Flight, UUID>, JpaSpecificationExecutor<Flight>` — methods: `existsByFlightNumber(String)`, `existsByFlightNumberAndIdNot(String, UUID)`
- **Files created/modified:**
  - `src/main/resources/schema.sql` — DDL for `flight` table with PK and unique constraints
  - `src/main/java/com/flightcontrol/domain/FlightStatus.java` — new
  - `src/main/java/com/flightcontrol/domain/Flight.java` — new
  - `src/main/java/com/flightcontrol/domain/OffsetDateTimeConverter.java` — new
  - `src/main/java/com/flightcontrol/repository/FlightRepository.java` — new
  - `src/test/java/com/flightcontrol/repository/FlightRepositoryTest.java` — new (11 `@DataJpaTest` tests)
  - `pom.xml` — added `<lombok.version>1.18.46</lombok.version>` property and explicit `maven-compiler-plugin` `annotationProcessorPaths` entry for Lombok
- **Decisions made:**
  - Lombok 1.18.46 pinned explicitly (overriding Spring Boot BOM's 1.18.34) because the runtime JDK is Java 25 (Zulu 25.0.2) and Lombok 1.18.34 throws `ExceptionInInitializerError: TypeTag::UNKNOWN` under Java 25. Version 1.18.46 (released March 2026) adds Java 25 support.
  - Added explicit `annotationProcessorPaths` in `maven-compiler-plugin` — Spring Boot's parent POM does not configure this automatically; without it, Lombok's annotation processor is not invoked and no getters/setters/builders are generated.
  - `OffsetDateTimeConverter` chosen over `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` because the explicit converter is transparent, testable, and avoids any Hibernate dialect quirks with H2.
  - `@DataJpaTest` test class uses `@TestPropertySource` to ensure `spring.sql.init.mode=always` and `spring.jpa.defer-datasource-initialization=true` so `schema.sql` is applied before Hibernate entity scanning in the test slice context.
- **Tests:** 12 passing (11 repository + 1 smoke), 0 failures (`./mvnw test` — BUILD SUCCESS in ~5 s)

---

## 2026-05-07 — Code Review (Increment 2)
- Quality: PASS (Critical: 0, High: 0, Medium: 4, Suggestion: 4)
- Coverage: GAPS FOUND — no test exercises `findAll(Specification<Flight>, Pageable)` with a real `Specification` predicate; filtering path is declared but untested
- Recommendation: fix and re-review

---

## 2026-05-07 — Increment 3: State Machine (Pure Domain)

- **What was completed:** `FlightStateMachine` pure-Java utility class encoding all allowed status transitions, and `InvalidStatusTransitionException` domain exception. Plain JUnit 5 test class exercises all 36 (6x6) transition combinations: 7 allowed pass, 29 disallowed throw. No Spring annotations in any production class.
- **Interfaces/methods created:**
  - `FlightStateMachine.validate(FlightStatus from, FlightStatus to)` — `void`; throws `InvalidStatusTransitionException` if transition not in allowed set
  - `FlightStateMachine.allowedTransitions(FlightStatus from)` — returns unmodifiable, non-null `Set<FlightStatus>`; empty set for terminal statuses (LANDED, CANCELLED)
  - `InvalidStatusTransitionException(FlightStatus from, FlightStatus to)` — `RuntimeException`; exposes `getFrom()` and `getTo()`; message format: `"Transition from {from} to {to} is not allowed"`
- **Files created/modified:**
  - `src/main/java/com/flightcontrol/domain/FlightStateMachine.java` — new
  - `src/main/java/com/flightcontrol/domain/exception/InvalidStatusTransitionException.java` — new
  - `src/test/java/com/flightcontrol/domain/FlightStateMachineTest.java` — new (44 tests)
  - `context/PROGRESS.md` — this update
  - `context/PLAN.md` — Increment 3 marked completed
- **Decisions made:**
  - `ALLOWED_TRANSITIONS` stored as `Collections.unmodifiableMap(EnumMap)` in a static initializer; inner sets are `EnumSet` instances wrapped with `Collections.unmodifiableSet` at query time — keeps mutation-safety without copying on every call.
  - Private constructor added to `FlightStateMachine` to prevent instantiation (pure utility class pattern).
  - Test generates disallowed pairs programmatically by computing the complement of the 7 defined allowed pairs within all 36 combinations — ensures the test is self-verifying (exhaustiveness assertion at the bottom confirms 36 = 7 + 29).
- **Tests:** 57 passing (44 state machine + 12 repository + 1 smoke), 0 failures (`./mvnw test` — BUILD SUCCESS in ~5.5 s)

---

## 2026-05-07 — Code Review (Increment 3)
- Quality: PASS (Critical: 0, High: 0, Medium: 2, Suggestion: 2)
- Coverage: FULLY COVERED
- Recommendation: approve — address 2 medium findings before Increment 5 wires callers through FlightStateMachine.validate (null guard on public inputs; string-keyed enum pair lookup in test disallowed-pair generator)

---

## 2026-05-07 — Increment 4: DTOs & Mapping

- **What was completed:** Four DTO classes with Bean Validation and snake_case JSON naming, plus a Spring `@Component` mapper with full entity↔DTO conversion and Spring Data `Page<Flight>` → `FlightPage` mapping. All 4 mapper tests pass; full test suite is 61 tests, 0 failures.
- **Interfaces/methods created:**
  - `FlightRequest` — Lombok `@Builder`; fields: `flightNumber` (`@NotBlank @Pattern(^[A-Z]{2}\d{3,4}$)`), `airline` (`@NotBlank @Size(1,100)`), `origin`/`destination` (`@NotBlank @Pattern(^[A-Z]{3}$)`), `departureTime`/`arrivalTime` (`@NotNull OffsetDateTime`); all with `@JsonProperty` snake_case names
  - `FlightResponse` — Lombok `@Builder`; all flight fields including `id`, `status`, `createdAt`, `updatedAt`; snake_case `@JsonProperty` on multi-word fields
  - `FlightPage` — Lombok `@Builder`; fields: `items`, `page`, `size`, `totalElements` (`@JsonProperty("total_elements")`), `totalPages` (`@JsonProperty("total_pages")`)
  - `StatusTransitionRequest` — single `@NotNull FlightStatus status` field
  - `FlightMapper.toResponse(Flight)` → `FlightResponse`
  - `FlightMapper.toEntity(FlightRequest)` → `Flight` (id/status/timestamps left null for `@PrePersist`)
  - `FlightMapper.toPage(Page<Flight>, int pageNum, int pageSize)` → `FlightPage`
- **Files created/modified:**
  - `src/main/java/com/flightcontrol/dto/FlightRequest.java` — new
  - `src/main/java/com/flightcontrol/dto/FlightResponse.java` — new
  - `src/main/java/com/flightcontrol/dto/FlightPage.java` — new
  - `src/main/java/com/flightcontrol/dto/StatusTransitionRequest.java` — new
  - `src/main/java/com/flightcontrol/mapper/FlightMapper.java` — new
  - `src/test/java/com/flightcontrol/mapper/FlightMapperTest.java` — new (4 plain JUnit 5 tests)
  - `context/PROGRESS.md` — this update
  - `context/PLAN.md` — Increment 4 marked completed
- **Decisions made:**
  - Used `jakarta.validation.*` (not `javax.validation.*`) throughout — Spring Boot 3 uses Jakarta EE 9+.
  - `FlightMapper` is a Spring `@Component` rather than a static utility to support DI and easier mocking in the service layer tests (Increment 5).
  - `FlightMapper` instantiated directly with `new FlightMapper()` in tests — no Spring context required.
  - Validation test uses `Validation.buildDefaultValidatorFactory()` in a try-with-resources block to ensure the factory is closed cleanly.
- **Tests:** 61 passing (4 mapper + 44 state machine + 12 repository + 1 smoke), 0 failures (`./mvnw test` — BUILD SUCCESS in ~5.6 s)

---

## 2026-05-07 — Code Review (Increment 4)
- Quality: PASS (Critical: 0, High: 0, Medium: 5, Suggestion: 2)
- Coverage: FULLY COVERED
- Recommendation: approve — address 5 medium findings before Increment 5 wires the service layer through FlightMapper

---

## 2026-05-07 — Code Review Fixes (Increment 4)

- **What was completed:** All 5 medium code-review findings addressed; 5 new tests added; full suite is 66 tests, 0 failures.
- **Changes made:**
  - `FlightMapper.java` — added `Objects.requireNonNull` guards at the top of `toResponse`, `toEntity`, and `toPage`; added `import java.util.Objects`
  - `FlightRequest.java` — removed redundant `@Size(min = 1)` (kept `@Size(max = 100)`); added explicit `@JsonProperty("airline")`, `@JsonProperty("origin")`, `@JsonProperty("destination")`
  - `FlightResponse.java` — added explicit `@JsonProperty` on `id`, `airline`, `origin`, `destination`, `status` (single-word fields that were previously relying on default serialisation)
  - `StatusTransitionRequest.java` — removed redundant `@JsonProperty("status")` annotation (field name already matched); added `@Builder` Lombok annotation; removed the now-unused `import com.fasterxml.jackson.annotation.JsonProperty`
  - `FlightMapperTest.java` — added 5 tests: `shouldPassValidation_whenAllFieldsAreValid`, `shouldRejectBlankAirline_whenAirlineIsEmpty`, `shouldRejectInvalidOrigin_whenOriginIsLowercase`, `shouldThrowNullPointerException_whenFlightIsNull`, `shouldThrowNullPointerException_whenRequestIsNull`; added `import static org.junit.jupiter.api.Assertions.assertThrows`
- **Tests:** 66 passing (9 mapper + 44 state machine + 12 repository + 1 smoke), 0 failures (`./mvnw test` — BUILD SUCCESS in ~5.6 s)

---

## 2026-05-07 — Increment 5: Service Layer & Business Rules

- **What was completed:** `FlightService` Spring `@Service` bean implementing all six use cases with full business-rule enforcement. Five domain exception classes created. 32 Mockito-based unit tests cover every happy path and every business-rule failure branch. `./mvnw test` — 98 tests, 0 failures, BUILD SUCCESS.
- **Interfaces/methods created:**
  - `FlightService(FlightRepository, FlightMapper)` — constructor-injected; usable with `new FlightService(mockRepo, mockMapper)` in tests
  - `FlightService.listFlights(String origin, String destination, FlightStatus status, int page, int size)` → `FlightPage` — builds `Specification<Flight>` from non-null filters; uses `PageRequest.of(page, size)`
  - `FlightService.getFlight(UUID id)` → `FlightResponse` — throws `FlightNotFoundException` if absent
  - `FlightService.createFlight(FlightRequest)` → `FlightResponse` — enforces departure-in-future, arrival>departure, origin≠destination, flight-number uniqueness
  - `FlightService.updateFlight(UUID id, FlightRequest)` → `FlightResponse` — additionally enforces SCHEDULED/DELAYED guard and flight-number uniqueness excluding self
  - `FlightService.deleteFlight(UUID id)` — enforces SCHEDULED-only guard
  - `FlightService.transitionStatus(UUID id, FlightStatus target)` → `FlightResponse` — delegates to `FlightStateMachine.validate`
  - `FlightNotFoundException(UUID id)` — `RuntimeException`; exposes `getId()`
  - `DuplicateFlightNumberException(String flightNumber)` — `RuntimeException`; exposes `getFlightNumber()`
  - `FlightNotModifiableException(UUID id, FlightStatus currentStatus)` — `RuntimeException`; exposes `getId()`, `getCurrentStatus()`
  - `FlightNotDeletableException(UUID id, FlightStatus currentStatus)` — `RuntimeException`; exposes `getId()`, `getCurrentStatus()`
  - `BusinessRuleViolationException(String message)` — `RuntimeException`
- **Files created/modified:**
  - `src/main/java/com/flightcontrol/domain/exception/FlightNotFoundException.java` — new
  - `src/main/java/com/flightcontrol/domain/exception/DuplicateFlightNumberException.java` — new
  - `src/main/java/com/flightcontrol/domain/exception/FlightNotModifiableException.java` — new
  - `src/main/java/com/flightcontrol/domain/exception/FlightNotDeletableException.java` — new
  - `src/main/java/com/flightcontrol/domain/exception/BusinessRuleViolationException.java` — new
  - `src/main/java/com/flightcontrol/service/FlightService.java` — new
  - `src/test/java/com/flightcontrol/service/FlightServiceTest.java` — new (32 tests)
  - `pom.xml` — added `maven-surefire-plugin` with `-Dnet.bytebuddy.experimental=true` argLine (required for Byte Buddy / Mockito to work under Java 25; this was the first increment to exercise `@Mock` annotations)
  - `context/PROGRESS.md` — this update
  - `context/PLAN.md` — Increment 5 marked completed
- **Decisions made:**
  - Business-rule checks are evaluated in a fixed order per method: status guard first (updateFlight/deleteFlight), then field validation rules, then uniqueness. This order means the cheapest/most-categorical check surfaces first.
  - `enforceBusinessRules` is a private shared helper called by both `createFlight` and `updateFlight` to avoid duplication.
  - The `buildSpecification` helper uses `Specification.where(null)` as the base so that an all-null filter call produces an unfiltered `findAll` — no special case needed.
  - `origin.equalsIgnoreCase(destination)` was chosen (case-insensitive) because the DTO enforces uppercase via `@Pattern`, but the service-layer check is defensive.
  - `@Transactional(readOnly = true)` applied to `listFlights` and `getFlight` for performance; class-level `@Transactional` covers mutations.
  - `net.bytebuddy.experimental=true` added to surefire argLine rather than as a project-wide JVM flag, keeping it scoped to the test phase only.
- **Tests:** 98 passing (32 service + 9 mapper + 44 state machine + 12 repository + 1 smoke), 0 failures (`./mvnw test` — BUILD SUCCESS in ~4.7 s)

---

## 2026-05-07 — Code Review Fixes (Increment 5)

- **What was completed:** All HIGH and MEDIUM code-review findings resolved; 2 missing tests added; full suite is 100 tests, 0 failures.
- **Changes made:**
  - `FlightStateMachine.java` — added null guard at the top of `validate(from, to)`: throws `IllegalArgumentException("Status arguments must not be null")` when either argument is null; added null guard at the top of `allowedTransitions(from)`: throws `IllegalArgumentException("Status must not be null")` when `from` is null
  - `FlightService.java` — added null guard at the start of `enforceBusinessRules`: throws `BusinessRuleViolationException("departure_time and arrival_time must not be null")` before any time-based checks
  - `FlightServiceTest.java` — added `shouldThrowBusinessRuleViolation_whenUpdateFlightArrivalTimeNotAfterDeparture` (updateFlight with `arrivalTime == departureTime`); added `shouldNotAddOriginPredicate_whenOriginIsBlank` (listFlights with `" "` as origin)
- **Tests:** 100 passing, 0 failures (`./mvnw test` — BUILD SUCCESS in ~4.7 s)

## 2026-05-07 — Increment 6: REST Controller & Error Handling

- **What was completed:** `RequestIdFilter`, `FlightController`, and `GlobalExceptionHandler` created. All 6 endpoints are wired to `FlightService`. RFC 9457 Problem Detail error responses with `application/problem+json` content-type, `X-Request-ID` echo on every response (success and error), and MDC correlation propagation into error bodies. 13 `@WebMvcTest` tests cover all endpoints, happy paths, and key error paths.
- **Interfaces/methods created:**
  - `RequestIdFilter extends OncePerRequestFilter` — reads/generates `X-Request-ID`, stores in `MDC("requestId")`, echoes header on every response; clears MDC in `finally`
  - `FlightController` — `GET /api/v1/flights` (listFlights), `POST /api/v1/flights` (createFlight → 201 + Location), `GET /api/v1/flights/{flightId}` (getFlight), `PUT /api/v1/flights/{flightId}` (updateFlight), `DELETE /api/v1/flights/{flightId}` (deleteFlight → 204), `PATCH /api/v1/flights/{flightId}/status` (transitionStatus)
  - `GlobalExceptionHandler` — `@RestControllerAdvice` handling `MethodArgumentNotValidException` (400), `HttpMessageNotReadableException` (400), `BusinessRuleViolationException` (422), `FlightNotFoundException` (404), `DuplicateFlightNumberException` (409), `FlightNotModifiableException` (409), `FlightNotDeletableException` (409), `InvalidStatusTransitionException` (409); all include `type`, `title`, `status`, `detail`, `instance`, `timestamp`, `correlation_id`; 400/422 include `errors` array
- **Files created/modified:**
  - `src/main/java/com/flightcontrol/web/RequestIdFilter.java` — new
  - `src/main/java/com/flightcontrol/web/FlightController.java` — new
  - `src/main/java/com/flightcontrol/web/GlobalExceptionHandler.java` — new
  - `src/test/java/com/flightcontrol/web/FlightControllerTest.java` — new (13 `@WebMvcTest` tests)
  - `context/PROGRESS.md` — this update
  - `context/PLAN.md` — Increment 6 marked completed
- **Decisions made:**
  - `@WebMvcTest` picks up `RequestIdFilter` and `GlobalExceptionHandler` via `@Import({RequestIdFilter.class, GlobalExceptionHandler.class})` on the test class — the filter is a `@Component` but `@WebMvcTest` does not auto-scan non-controller components.
  - `BusinessRuleViolationException` produces a single-entry `errors` array with `field=""`, `code="business_rule_violation"` since it has no structured field binding.
  - `MDC.clear()` is called in `finally` in `RequestIdFilter` to prevent MDC leakage across requests in a thread-pooled container.
  - `GlobalExceptionHandler` reads `requestId` from `MDC` (set by the filter before the handler chain runs) rather than from `HttpServletRequest` directly, ensuring the same value is echoed even for filter-bypassed error scenarios.
- **Tests:** 113 passing (13 controller + 34 service + 9 mapper + 44 state machine + 12 repository + 1 smoke), 0 failures (`./mvnw test` — BUILD SUCCESS in ~6.5 s)

---

## 2026-05-07 — Code Review (Increment 5)
- Quality: FAIL (Critical: 0, High: 1, Medium: 4, Suggestion: 2)
- Coverage: GAPS FOUND — `updateFlight` missing test for `arrival_time <= departure_time` business rule failure
- Recommendation: fix and re-review

### Findings

**High**
- `FlightStateMachine.validate` has no null guard on `from`; if the persisted entity status is null, `ALLOWED_TRANSITIONS.get(null)` returns null and the subsequent `.contains()` call throws a context-free NullPointerException. The service call site at `FlightService.java:99` does not guard against this. (Flagged in Increment 3 review; now wired into a production call path.)

**Medium**
1. `enforceBusinessRules` calls `request.getDepartureTime().isAfter(now)` and `request.getArrivalTime().isAfter(...)` without null checks (FlightService.java:115, 120). Bean Validation guards this at the controller boundary but not when the service is called directly.
2. `scheduledFlight` shared in `@BeforeEach` is a mutable entity; `updateFlight` tests mutate it in-place via setters. Tests are safe today because `@BeforeEach` rebuilds the instance, but the pattern is fragile (FlightServiceTest.java:66–77).
3. `updateFlight` test suite does not include a case for `arrival_time <= departure_time` rule failure (FlightServiceTest.java). Every other business rule has a corresponding test for this method.
4. `listFlights` test suite does not cover the blank-string origin branch (FlightService.java:134 `!origin.isBlank()` guard is untested).

**Suggestion**
1. `BusinessRuleViolationException` carries only a free-form message string with no structured violation-type field; the GlobalExceptionHandler in Increment 6 will need to string-parse or use a single catch-all type URI.
2. `transitionStatus` happy-path test covers only SCHEDULED → DELAYED; at least one additional allowed pair should be exercised to confirm delegation is correct end-to-end.

---

## 2026-05-07 — Code Review Re-review (Increment 5)
- Quality: PASS (Critical: 0, High: 0)
- Coverage: FULLY COVERED
- Recommendation: approve

## 2026-05-07 — Test Run (Increment 6, Attempt 1)
- Passed: 113 | Failed: 0 | Skipped: 0
- Build: SUCCESS
- Coverage: Not configured (coverage gate: SKIPPED — no JaCoCo plugin in pom.xml)
- Test Duration: 5.0s
- Overall Duration: 6.4s
- Failures: None
- Action: All clear — Test suite fully passing. Ready for Increment 7.

## 2026-05-07 — Code Review (Increment 6)
- Quality: FAIL (Critical: 0, High: 2, Medium: 7, Suggestion: 3)
- Coverage: GAPS FOUND — 4 of 8 exception handlers untested at controller slice (BusinessRuleViolationException/422, DuplicateFlightNumberException/409, FlightNotModifiableException/409, FlightNotDeletableException/409); PUT and DELETE error paths have no test
- Recommendation: fix and re-review

## 2026-05-07 — Code Review Fixes (Increment 6)

- **What was completed:** All HIGH, MEDIUM, and SUGGESTION findings from the Increment 6 code review resolved. 7 new tests added, 1 duplicate test removed. Final suite: 119 tests, 0 failures.
- **Changes made:**
  - `FlightController.java` — added `@Validated` on class; added `@Min(0)` on `page` parameter; added `@Min(1) @Max(100)` on `size` parameter; added imports for `jakarta.validation.constraints.Max`, `Min`, and `org.springframework.validation.annotation.Validated`
  - `GlobalExceptionHandler.java` — added `ConstraintViolationException` handler (maps to 400, `validation-failed` type, includes `errors` array with field/code/message extracted from constraint violations); added `MethodArgumentTypeMismatchException` handler (maps to 400, `validation-failed`, handles invalid enum query params like `?status=BOGUS`); added null MDC guard in `buildProblem` (`requestId` falls back to a fresh UUID if MDC has no value); added catch-all `Exception` handler returning 500 with `application/problem+json`, `internal-error` type; added `extractFieldName` helper to strip method path prefix from `ConstraintViolation.getPropertyPath()`; added imports for `ConstraintViolation`, `ConstraintViolationException`, `MethodArgumentTypeMismatchException`, `UUID`, `Collectors`
  - `RequestIdFilter.java` — changed `response.addHeader(...)` to `response.setHeader(...)` to prevent duplicate `X-Request-ID` response headers
  - `FlightControllerTest.java` — added imports for `BusinessRuleViolationException`, `DuplicateFlightNumberException`, `FlightNotDeletableException`, `FlightNotModifiableException`; removed duplicate test `shouldIncludeAllRequiredProblemDetailFields_whenFlightNotFound`; added 7 new tests: `shouldReturn422WithErrorsArray_whenCreateFlightViolatesBusinessRule`, `shouldReturn409WithDuplicateType_whenCreateFlightHasDuplicateNumber`, `shouldReturn409WithNotModifiableType_whenUpdateFlightIsNotModifiable`, `shouldReturn404_whenDeleteFlightNotFound`, `shouldReturn409WithNotDeletableType_whenDeleteFlightIsNotDeletable`, `shouldReturn400_whenTransitionStatusBodyIsInvalid`, `shouldReturn400_whenUpdateFlightBodyIsInvalid`
- **Tests:** 119 passing, 0 failures (`./mvnw test` — BUILD SUCCESS in ~5.0 s)

## 2026-05-07 — Code Review Re-review (Increment 6)
- Quality: PASS (Critical: 0, High: 0, Medium: 2)
- Coverage: GAPS FOUND — 3 of the 5 newly added handlers have no test coverage: `ConstraintViolationException` (no test for `?page=-1` or `?size=200` → 400 + errors array), `MethodArgumentTypeMismatchException` (no test for `?status=BOGUS` → 400 + validation-failed), catch-all `Exception` handler (no test verifying 500 + `internal-error` type)
- Recommendation: fix and re-review

## 2026-05-07 — Code Review Fixes (Increment 6, Round 2)

- **What was completed:** All remaining MEDIUM findings from the Increment 6 re-review resolved. 3 new tests added. Final suite: 122 tests, 0 failures.
- **Changes made:**
  - `GlobalExceptionHandler.java` — (Fix A) replaced `ex.getValue()` with `Objects.toString(ex.getValue(), "<missing>")` in `handleMethodArgumentTypeMismatch` to prevent rendering the literal string `"null"` when the value is null; added `import java.util.Objects`; (Fix B) added `private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class)` field; added `log.error("Unexpected error processing request", ex)` as first line of `handleUnexpected`; added `import org.slf4j.Logger` and `import org.slf4j.LoggerFactory`
  - `FlightControllerTest.java` — added `@Import(MethodValidationPostProcessor.class)` to the `@Import` annotation so `@Validated` + `@Max(100)` on `FlightController.listFlights` triggers `ConstraintViolationException` inside the `@WebMvcTest` slice; added `import org.springframework.validation.beanvalidation.MethodValidationPostProcessor`; added `import static org.mockito.ArgumentMatchers.anyInt`; added 3 new tests: `shouldReturn400_whenSizeExceedsMaximum`, `shouldReturn400_whenStatusQueryParamIsInvalid`, `shouldReturn500WithProblemJson_whenUnexpectedExceptionOccurs`
- **Decisions made:**
  - `MethodValidationPostProcessor` must be imported explicitly in `@WebMvcTest` slices because `@WebMvcTest` only bootstraps the web layer and does not auto-configure method-level bean validation. Adding it to `@Import` activates the AOP proxy that enforces `@Validated` constraints on controller methods.
  - The `shouldReturn400_whenSizeExceedsMaximum` test works correctly with this import — `GET /api/v1/flights?size=200` returns 400 with `application/problem+json` and `$.type` containing `validation-failed`.
- **Tests:** 122 passing, 0 failures (`./mvnw test` — BUILD SUCCESS in ~6.4 s)

---

## 2026-05-07 — Increment 7: Integration Test

- **What was completed:** `FlightLifecycleIntegrationTest` — a `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureMockMvc` test class with 12 ordered test methods covering the complete flight lifecycle against a real H2 database. All 12 scenarios pass. Full suite is 134 tests, 0 failures.
- **Interfaces/methods created:** None (test-only increment).
- **Files created/modified:**
  - `src/test/java/com/flightcontrol/integration/FlightLifecycleIntegrationTest.java` — new; 12 `@Test @Order(N)` methods sharing state via `static UUID` fields
  - `context/PROGRESS.md` — this update
  - `context/PLAN.md` — Increment 7 marked completed
- **Decisions made:**
  - `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` with individual `@Test @Order(N)` methods chosen over a single monolithic test for readability and targeted failure reporting.
  - Shared flight IDs stored in `static UUID` fields (`createdFlightId`, `secondFlightId`) so ordered methods can reference the flight created in earlier steps.
  - `MockMvc` used instead of `TestRestTemplate` — supports fluent `jsonPath` assertions and header assertions without deserialising into POJOs.
  - `ObjectMapper` autowired for serialising `FlightRequest` to JSON strings, ensuring Jackson config (snake_case, UTC timestamps) matches what the controller expects.
  - `@SpringBootTest` with `RANDOM_PORT` loads the full application context including `schema.sql`, all beans, and the embedded H2 datasource from `application.properties` — no test-specific overrides required.
- **Tests:** 134 passing (12 integration + 22 controller + 34 service + 9 mapper + 44 state machine + 12 repository + 1 smoke), 0 failures (`./mvnw test` — BUILD SUCCESS in ~6.7 s)

## 2026-05-07 — Code Review (Increment 7)
- Quality: PASS (Critical: 0, High: 0, Medium: 4)
- Coverage: GAPS FOUND — Step 3 (listFlights) sends no filter parameters; filter specification path is unexercised end-to-end; X-Request-ID not asserted on the Order 12 POST response
- Recommendation: fix and re-review
