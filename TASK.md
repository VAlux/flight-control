# Flight Control System

## Overview

Build a **Flight Control System** — a full-stack application with a Spring Boot backend and a **vanilla HTML/CSS/JavaScript** frontend (no frameworks). The system manages flights for a small airline: create, update, delete, search, and transition flight statuses through a defined state machine.

---

## Tech Stack

| Layer    | Technology                                                   |
| -------- | ------------------------------------------------------------ |
| Backend  | Java 17, Spring Boot 3.3, Spring Data JPA, Spring Validation |
| Database | H2 (dev), schema managed via `data.sql`                     |
| API Docs | springdoc-openapi (Swagger UI at `/swagger-ui.html`)         |
| Frontend | Vanilla HTML5 + CSS3 + ES6 JavaScript (no frameworks)        |
| Build    | Maven Wrapper (`./mvnw`)                                     |
| Testing  | JUnit 5, Mockito, Spring MockMvc                             |

---

## Business Rules

1. Flights cannot be created with `departure_time` in the past
2. `arrival_time` must be after `departure_time`
3. `origin` and `destination` must differ
4. `flight_number` must be unique across all flights
5. Flight updates (PUT) are only allowed when status is `SCHEDULED` or `DELAYED`
6. Flight deletion is only allowed when status is `SCHEDULED`
7. Flight status transitions must follow this state machine:
   - `SCHEDULED` → `DELAYED`
   - `SCHEDULED` → `DEPARTED`
   - `SCHEDULED` → `CANCELLED`
   - `DELAYED` → `DEPARTED`
   - `DELAYED` → `CANCELLED`
   - `DEPARTED` → `IN_AIR`
   - `IN_AIR` → `LANDED`
   - No other transitions allowed
8. `CANCELLED` and `LANDED` are terminal states — no further transitions
---

## Frontend (Vanilla UI)

The frontend is served as **static files** from `src/main/resources/static/`. No bundlers, no frameworks — just HTML, CSS, and ES6 JavaScript using `fetch()`.

### Pages

| Page          | File               | Description                                                    |
| ------------- | ------------------ | -------------------------------------------------------------- |
| Flight List   | `index.html`       | Main page: searchable/filterable flight table, create modal    |
| Flight Detail | `flight-detail.html` | Flight info, status transition controls, edit/delete actions |

### UI Requirements
- Responsive layout using CSS Grid/Flexbox (no CSS frameworks)
- Loading spinners during API calls
- Toast notifications for success/error feedback
- Confirmation dialogs for destructive actions (delete, cancel flight)
- Form validation matching backend constraints
- Pagination controls for the list view
- Status displayed as colored badges
- Status transition buttons shown based on allowed transitions

---