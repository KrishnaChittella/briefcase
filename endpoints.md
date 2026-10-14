# Briefcase API Endpoints

Base URL: `http://localhost:8080`

All routes require a `Authorization: Bearer <token>` header (obtained from
`POST /api/auth/login`) except the `auth` endpoints and Actuator health/info.
Role names refer to `com.krishna.briefcase.common.security.Roles`:
- **ADMIN** = SUPER_ADMIN or MANAGING_PARTNER
- **ATTORNEY** = ADMIN + SENIOR_ATTORNEY + JUNIOR_ATTORNEY
- **STAFF_OR_ABOVE** = ATTORNEY + STAFF
- **authenticated** = any logged-in user, no specific role required

| Method | Endpoint | Access | What it does |
|--------|----------|--------|---------------|
| POST | `/api/auth/register` | public | Registers a new user account (username, password, role) and returns a JWT. |
| POST | `/api/auth/login` | public | Authenticates username/password and returns a JWT. |
| POST | `/api/clients` | STAFF_OR_ABOVE | Creates a new client (client intake). |
| GET | `/api/clients` | authenticated | Lists all clients. |
| GET | `/api/clients/{id}` | authenticated | Fetches a single client by id. |
| PUT | `/api/clients/{id}` | STAFF_OR_ABOVE | Updates a client's details. |
| DELETE | `/api/clients/{id}` | ADMIN | Deletes a client. |
| POST | `/api/attorneys` | ADMIN | Creates a new attorney (firm headcount) record. |
| GET | `/api/attorneys` | authenticated | Lists all attorneys. |
| GET | `/api/attorneys/me` | authenticated | Returns the attorney profile linked to the caller's own login, if any. |
| GET | `/api/attorneys/{id}` | authenticated | Fetches a single attorney by id. |
| PUT | `/api/attorneys/{id}` | ADMIN | Updates an attorney record (including linking to a login user via `userId`). |
| DELETE | `/api/attorneys/{id}` | ADMIN | Deletes an attorney record. |
| POST | `/api/cases` | STAFF_OR_ABOVE | Creates a new case (basic intake, no assignments). |
| GET | `/api/cases` | authenticated | Paginated + filterable case search (`status`, `billingType`, `clientId`, `attorneyId`, `openedFrom`, `openedTo`, `q`, `page`, `size`, `sort`). |
| GET | `/api/cases/{id}` | authenticated | Fetches a single case by id. |
| PUT | `/api/cases/{id}` | ATTORNEY | Updates a case's substantive details (title, billing config, etc). `billingType` is CONTINGENCY (needs `contingencyRate`), HOURLY (needs `hourlyRate`) or FLAT_FEE (needs `flatFee`). |
| PATCH | `/api/cases/{id}/status` | ATTORNEY | Transitions a case's lifecycle status (OPEN → IN_PROGRESS → RESOLVED → CLOSED), validated against allowed transitions. |
| DELETE | `/api/cases/{id}` | ADMIN | Deletes a case. |
| POST | `/api/cases/onboard` | ATTORNEY | Builder-pattern shortcut: creates a case **and** assigns its initial attorney team (lead/associates/paralegals) in one atomic call. |
| POST | `/api/cases/{caseId}/assignments` | ATTORNEY | Assigns an attorney to the case with a role (LEAD/ASSOCIATE/PARALEGAL); enforces exactly one LEAD per case. |
| GET | `/api/cases/{caseId}/assignments` | authenticated | Lists attorneys currently assigned to the case. |
| DELETE | `/api/cases/{caseId}/assignments/{attorneyId}` | ATTORNEY | Unassigns an attorney from the case. |
| POST | `/api/cases/{caseId}/documents` | STAFF_OR_ABOVE | Uploads a document (multipart file) attached to the case. |
| GET | `/api/cases/{caseId}/documents` | authenticated | Lists document metadata for the case. |
| GET | `/api/cases/{caseId}/documents/{documentId}/download` | authenticated | Downloads a document's raw file content. |
| POST | `/api/cases/{caseId}/outcome` | ATTORNEY | Records a case's legal result (WON/LOST/SETTLED) and triggers the fee-distribution engine (revenue calc, firm cut, attorney split) — one-time, idempotency-guarded. |
| GET | `/api/cases/{caseId}/outcome` | authenticated | Fetches the recorded outcome for a case (result + financial breakdown). |
| GET | `/api/cases/{caseId}/fee-distributions` | authenticated | Lists each attorney's fee share for a resolved case. |
| GET | `/api/cases/{caseId}/audit-log` | ADMIN | Lists the change history (status changes, assignment changes, fee calculation) for a case. |
| GET | `/api/leaderboard` | authenticated | Top Attorney leaderboard; query params `metric` (REVENUE\|WIN_RATE\|CASES_LED, default REVENUE) and `period` (MONTH\|QUARTER\|YEAR\|ALL_TIME, default ALL_TIME). |
