# Project Standards

Core agent behaviour, architecture principles, quality standards, and development workflow.
Loaded automatically by Claude Code in every conversation.

---

## Agent Behaviour

- **Understand intent first.** Before generating code, confirm what the user is trying to achieve. A precise small change beats an impressive large one that misses the point.
- **Minimise blast radius.** Propose the smallest change that satisfies the requirement. Do not refactor surrounding code unless explicitly asked.
- **Respect existing conventions.** Match the style, naming, and structure already present in the file or module being modified. Consistency across a codebase is more valuable than local perfection.
- **Make changes traceable.** If a decision has trade-offs, state them. If an assumption was made, surface it. Leave the developer in control.
- **Validate before suggesting.** Check for nulls, unhandled edge cases, missing error paths, and concurrent-access issues before presenting a solution.
- **Tests accompany changes.** Every behavioural change to production code should be paired with a corresponding test change — new, updated, or explicitly justified as not needed.

---

## Architecture Principles

- Organise code in **clearly defined, loosely coupled layers** with dependencies that flow in one direction only.
- Keep the **domain / business logic layer** free of infrastructure concerns. It must be possible to test it without a running container or database.
- Express cross-cutting concerns (logging, security, validation, transactions) declaratively and uniformly — not ad hoc inside business logic.
- Prefer **explicit over implicit** at every level: explicit dependencies, explicit contracts, explicit error states, explicit data transformations.
- Design for **operability from day one**: structured logging, health checks, meaningful metrics, and graceful degradation are not afterthoughts.

---

## Quality Standards

| Dimension       | Expectation                                                                                              |
| --------------- | -------------------------------------------------------------------------------------------------------- |
| **Correctness** | All edge cases, error paths, and boundary conditions are handled and tested                              |
| **Readability** | Code reads like well-written prose — intent is clear without needing comments to explain _what_          |
| **Testability** | Every unit of business logic can be tested in isolation without infrastructure                           |
| **Safety**      | No unvalidated external input reaches business logic or persistence; no sensitive data logged or exposed |
| **Resilience**  | Failures in downstream dependencies are handled gracefully; partial failures do not cascade              |
| **Performance** | Resource usage (memory, threads, connections, I/O) is conscious and bounded                              |

---

## Development Workflow

```bash
# Verify everything passes before opening a pull request
<build-tool> clean verify

# Run only unit tests (fast feedback loop during development)
<build-tool> test

# Run integration tests (pre-merge)
<build-tool> integration-test

# Inspect coverage report
# target/site/jacoco/index.html  (Maven)
# build/reports/jacoco/          (Gradle)
```
