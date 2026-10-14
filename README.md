# Briefcase

A Spring Boot backend for a law firm case management system. A firm manages legal
cases; each case has a client, an assigned team of attorneys (one LEAD, any number of
associates and paralegals), and — once resolved — a financial outcome. Revenue is
computed automatically (contingency, hourly or flat-fee billing), the firm takes its cut, the
lead attorney takes a fixed cut of what's left, and the remainder splits equally among
the rest of the team. A leaderboard ranks attorneys by revenue, win rate, or cases led.

Built as a demonstration of production backend patterns: layered architecture, DTO
boundaries, JWT + RBAC, transactional integrity, optimistic locking, an event-driven
audit trail, and six classic design patterns applied where they actually earn their
keep — not bolted on for show.

## Tech stack

- **Java 21**, **Spring Boot 4.1** (Spring Framework 7 / Jakarta EE 11)
- **PostgreSQL** + **Flyway** for versioned schema migrations
- **Spring Data JPA** / Hibernate
- **Spring Security** + **JJWT** for stateless JWT authentication
- **Bean Validation**, **Maven**

## Architecture

Package-by-feature, not package-by-layer — each business capability owns its full
vertical slice (entity, repository, service, controller, DTOs):

```
common/        cross-cutting: ApiError, GlobalExceptionHandler, exceptions, BaseEntity,
               Roles (RBAC constants), CurrentUser
auth/          User, UserRole, JWT issue/verify, SecurityConfig
client/        Client CRUD
attorney/      Attorney CRUD, linked 1:1 to a login User
cases/         LegalCase, status lifecycle, assignments, search, CaseBuilder
outcome/       CaseOutcome, CaseResult, CaseOutcomeRecordedEvent
billing/       BillingCalculationStrategy, FeeSplitStrategy, CaseFinancialResolver,
               FeeDistribution
audit/         AuditLog + listener + admin read endpoint
notification/  NotificationListener
leaderboard/   RankingStrategy, RankingStrategyFactory, leaderboard endpoint
storage/       FileStorageAdapter, LocalFileStorageAdapter, Document
```

Every entity is hidden behind a DTO at the controller boundary — JPA entities never
leave the service layer. Every controller mutation route is gated by `@PreAuthorize`
with role expressions from a single `Roles` constants class, so authorization rules
live in one place instead of being scattered as string literals.

## Design patterns

Six patterns show up in this codebase, each solving a real problem rather than being
included for the resume line:

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `BillingCalculationStrategy` (Contingency / Hourly / FlatFee), `FeeSplitStrategy`, `RankingStrategy` (Revenue / WinRate / CasesLed) | Swap an algorithm without touching the code that calls it. |
| **Factory** | `BillingStrategyFactory`, `RankingStrategyFactory` | Picks the right Strategy bean at runtime from an enum, so callers never `instanceof`/`switch` over concrete types. |
| **Template Method** | `CaseFinancialResolver.resolve()` | Fixes the skeleton of "resolve a case financially" while letting each step vary independently. |
| **Builder** | `CaseBuilder` | Fluent construction of a case plus its initial attorney team in one call. |
| **Observer** | Spring `ApplicationEvent`s → `AuditLogListener` + `NotificationListener` | Decouples "a case changed" from "who cares and what they do about it." |
| **Adapter** | `FileStorageAdapter` → `LocalFileStorageAdapter` | Document storage backend is swappable (local disk today, S3 later) without touching `DocumentService`. |

### The problem that led to Strategy + Factory

Case revenue isn't computed the same way twice. A contingency case pays out a
percentage of the settlement; an hourly case pays out logged hours × rate. Bolting
both onto one method — `if (case.getBillingType() == CONTINGENCY) { ... } else { ... }`
— works fine for two billing types. It stops working the moment a third type shows up
(retainer? success bonus?), because now every place that branches on `BillingType` needs a
new `else if`, and it's easy to miss one.

I pulled the "how do I calculate revenue" question out into `BillingCalculationStrategy`,
one implementation per billing type, and let `BillingStrategyFactory` pick the right
bean at runtime by looking up `BillingType` in a map built from `List<BillingCalculationStrategy>`
at startup. Adding a new billing type is now: write a class, annotate it `@Component`,
done — `CaseFinancialResolver` never changes. The exact same shape solves fee-splitting
(`FeeSplitStrategy`) and leaderboard ranking (`RankingStrategy`): one algorithm family
per concept, one factory per family, zero conditional sprawl in the orchestrating code.

### A real bug: the audit log that silently wrote nothing

Wiring the Observer pattern for the audit trail looked done — `AuditLogListener` used
`@TransactionalEventListener(phase = AFTER_COMMIT)` so a log entry is only written if
the underlying change actually committed (no point auditing a rolled-back operation).
It compiled, the app booted, `NotificationListener`'s sibling handler for the same
event logged correctly. Then I hit the audit-log endpoint after assigning an attorney
to a case — empty array. No exception in the logs. No SQL insert anywhere. The
listener had clearly run (I could log a line at the top of the method), but the
`repository.save()` call inside it vanished into nothing.

The cause: right after a transaction commits, Spring hasn't finished clearing that
transaction's thread-bound resources yet when `AFTER_COMMIT` synchronizations run. A
plain `@Transactional` repository call made from inside that callback doesn't start a
fresh transaction — it silently tries to *participate* in the transaction that just
died, so the write never reaches the database, and nothing about that failure is loud
enough to show up as an exception. The fix is one annotation:
`@Transactional(propagation = Propagation.REQUIRES_NEW)` on each listener method,
forcing a genuinely new transaction instead of joining the ghost of the old one. Confirmed
by re-running the same request and watching the row actually appear.

## SOLID, concretely

- **Single Responsibility** — `CaseService` (case CRUD), `CaseAssignmentService`
  (team assignment), `CaseOutcomeService` (recording results), and
  `CaseFinancialResolver` (fee math) are four separate classes instead of one
  1000-line `CaseService` god-class. Each has exactly one reason to change.
- **Open/Closed** — Adding a billing type, a fee-split rule, or a leaderboard metric
  means writing a new `@Component` implementing the relevant Strategy interface.
  `CaseFinancialResolver`, `BillingStrategyFactory`, and `LeaderboardController` are
  never touched.
- **Liskov Substitution** — Every `BillingCalculationStrategy` / `FeeSplitStrategy` /
  `RankingStrategy` implementation is fully interchangeable through its interface;
  `CaseFinancialResolver` has no idea which concrete strategy it's holding.
- **Interface Segregation** — `FileStorageAdapter` exposes exactly `store` / `load` /
  `delete` — nothing a caller doesn't need, instead of one bloated `StorageService`
  interface mixing upload, thumbnailing, virus scanning, etc.
- **Dependency Inversion** — High-level orchestration (`CaseFinancialResolver`,
  `DocumentService`) depends on interfaces (`BillingCalculationStrategy`,
  `FeeSplitStrategy`, `FileStorageAdapter`), never on concrete implementations.
  Spring wires the concrete beans in; the orchestrators would work unmodified if
  every implementation were swapped out.

## How fee distribution works

`POST /api/cases/{id}/outcome` is the single entry point, wrapped in one
`@Transactional` boundary (`CaseOutcomeService.recordOutcome`):

1. **Guard** — the case must be in `RESOLVED` status and must not already be
   `financiallyResolved` (idempotency: the same case can't be paid out twice).
2. **`CaseFinancialResolver.resolve()`** runs the fixed Template Method skeleton:
   - `computeRevenue` — delegates to whichever `BillingCalculationStrategy` matches
     the case's `BillingType` (contingency: settlement × rate; hourly: hours × rate;
     flat fee: the agreed fee).
   - `applyFirmCut` — the firm takes `billing.firm-cut-rate` (config, default 40%) off
     the top.
   - `splitAmongAttorneys` — delegates to `FeeSplitStrategy`: the LEAD takes
     `billing.lead-cut-rate` (default 40%) of what's left; the remainder splits
     equally among ASSOCIATEs; PARALEGALs are excluded entirely; if there are no
     ASSOCIATEs, the LEAD takes the whole remainder. Rounding remainders are folded
     into the lead's cut so distributed shares always sum exactly to the
     net-distributable amount — no stray cents.
   - `persist` — writes one `FeeDistribution` row per attorney.
3. **Idempotency is backstopped twice**: the `financiallyResolved` flag on the case,
   and a unique `(case_id, attorney_id)` constraint on `fee_distribution` at the
   database level, so even a bug in the flag check can't double-pay anyone.
4. **Optimistic locking** (`@Version` on `LegalCase`) protects against two people
   editing or resolving the same case concurrently — the loser gets a clean 409
   instead of a silently lost update.

## How the leaderboard works

`GET /api/leaderboard?metric=REVENUE&period=ALL_TIME` — both params optional.

- `RankingStrategyFactory` picks the `RankingStrategy` bean matching the requested
  `RankingMetric` (`REVENUE`, `WIN_RATE`, or `CASES_LED`).
- `LeaderboardPeriod` (`MONTH` / `QUARTER` / `YEAR` / `ALL_TIME`) resolves to a single
  UTC "since" `Instant` — `ALL_TIME` uses `Instant.EPOCH` as a uniform lower bound, so
  every query is just "since X" with no branching for the unbounded case.
- Each strategy runs a read-only JPQL aggregation against `LeaderboardRepository`:
  - **Revenue** sums `FeeDistribution.amount` per attorney since the period start.
  - **Win rate** pulls `(attorneyId, CaseResult)` pairs for every LEAD/ASSOCIATE
    assignment on a case with an outcome since the period start, then computes
    `wins / total × 100` in Java.
  - **Cases led** counts LEAD assignments on cases with an outcome since the period
    start.
- `AttorneyNameLookup` batch-resolves display names for every strategy in one query,
  avoiding an N+1 lookup per row.
- Results are sorted descending by score and returned with a 1-indexed rank.

## Security model

JWT bearer auth, stateless sessions. Roles: `SUPER_ADMIN`, `MANAGING_PARTNER`,
`SENIOR_ATTORNEY`, `JUNIOR_ATTORNEY`, `PARALEGAL`, `STAFF` — collapsed into three
`@PreAuthorize` tiers in `Roles`:

- **ADMIN** (`SUPER_ADMIN`, `MANAGING_PARTNER`) — headcount management, deletions.
- **ATTORNEY** (+ `SENIOR_ATTORNEY`, `JUNIOR_ATTORNEY`) — substantive case edits, status
  transitions, assignments, recording outcomes.
- **STAFF_OR_ABOVE** (+ `STAFF`) — intake: creating clients and cases, uploading
  documents.

Reads are open to any authenticated user; writes are role-gated. Full endpoint
reference: see [`endpoints.md`](endpoints.md).

## Running locally

Requires Java 21 and a running PostgreSQL instance.

```bash
# One-time database setup
psql -d postgres -c "CREATE ROLE briefcase_app LOGIN PASSWORD 'choose-a-password';"
createdb -O briefcase_app briefcase

# Local settings: copy the template and fill in the password and a JWT secret
cp .env.example .env
openssl rand -base64 32   # paste as JWT_SECRET

./mvnw spring-boot:run
```

`.env` is git-ignored and loaded automatically through `spring.config.import`, so no secrets
live in the repository. Flyway builds the schema on first boot.

Open **http://localhost:8080** for the web UI (sign in or create an account), or call the
API directly; see [`endpoints.md`](endpoints.md).

```bash
./mvnw test
```
