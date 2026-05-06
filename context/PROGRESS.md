# Progress Log

## 2026-05-07 — Planning Complete

9 increments defined. No code written yet.

### Increment Status

| # | Title | Status |
|---|---|---|
| 1 | Project Scaffold & Build | completed |
| 2 | Domain Model & Persistence Layer | completed |
| 3 | State Machine (Pure Domain) | completed |
| 4 | DTOs & Mapping | pending |
| 5 | Service Layer & Business Rules | pending |
| 6 | REST Controller & Error Handling | pending |
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
