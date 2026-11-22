# RCS Mahesh — Claim Allocation API

Allocates a claim to an eligible claim handler, and shows which claim sits with which handler.

Spring Boot 3.3 · Java 17 · PostgreSQL

## API flow

```
Client / External Claim System    sends claim details
          ↓
REST API Controller               receives and validates requests   (controller/)
          ↓
Service Layer                     business logic and eligibility rules (service/)
          ↓
Repository Layer                  database queries and persistence  (repository/)
          ↓
Database (PostgreSQL)             claim_handler, claim, handler_mapping, handler_leave, allocation
```

**One handler, many locations and policy numbers.** These are stored in `handler_mapping`, one row per value:

| handler | mapping_type | mapping_value |
|---|---|---|
| CH001 | LOCATION | PUNE |
| CH001 | LOCATION | MUMBAI |
| CH004 | POLICY_NUMBER | POL-5001 |
| CH004 | POLICY_NUMBER | POL-5002 |

All request bodies are in one file, `dto/RcsDto.java`: `ClaimRequest`, `HandlerRequest`, `MappingRequest` and `LeaveRequest`.

## Run

> **Upgrading from the earlier version?** The `claim_handler` table changed: the single `claim_location`
> column moved into `handler_mapping`. Recreate the database once with
> `DROP DATABASE rcs; CREATE DATABASE rcs;`

1. Create the database in pgAdmin or psql: `CREATE DATABASE rcs;`
2. Set your PostgreSQL password in `src/main/resources/application.yml`.
3. Start the app:

```bash
mvn spring-boot:run
```

Hibernate creates the tables. `data.sql` loads four handlers and two claims, and it's safe to run on every start.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/claims/allocate` | Client sends claim details; the claim is saved and allocated |
| GET | `/api/claims/{claimNumber}` | One claim with its handler |
| GET | `/api/claims` | All claims with their handlers |
| GET | `/api/claims/handler/{handlerId}` | One handler's claims |
| POST | `/api/handlers` | Create a handler |
| PUT | `/api/handlers/{code}` | Update a handler (the lists you send replace the old ones) |
| GET | `/api/handlers` | Fetch all handlers |
| GET | `/api/handlers/{code}` | Fetch one handler |
| POST | `/api/handlers/{code}/mappings` | "Add" more locations, policy numbers or insured names to a handler |
| POST | `/api/handlers/{code}/leaves` | Mark a handler on leave or half day |

## How allocation works

1. Save the claim. If it was already allocated, return the same handler.
2. Keep the eligible handlers:
   - Active
   - LOB matches
   - Amount is within the handler's range
   - Claim location is one of the handler's locations
   - Policy number is one of the handler's policy numbers (if it has none, it takes any policy)
   - Insured name matches the same way
   - Not on leave or half day right now
3. Of those, allocate to whoever has the fewest open claims.

## Try it

**Starting state:** CH001 (PUNE, MUMBAI) has 2 open claims and CH002 (PUNE, NASHIK) has 0. CH003 (PUNE) is on leave.
CH004 (PUNE, MUMBAI; policies POL-5001 and POL-5002) is inactive.

```
POST http://localhost:8080/api/claims/allocate
{
  "claimNumber": "CLM-001",
  "claimLocation": "PUNE",
  "lob": "MOTOR",
  "policyNumber": "POL-1234",
  "insuredName": "AMIT PATIL",
  "lossReserveAmount": 25000
}
→ goes to CH002

Same body with "claimNumber": "CLM-002"                → CH002 again (1 claim vs CH001's 2)
Same body with "claimLocation": "MUMBAI", new number   → CH001 (only active handler with MUMBAI)
Same body with "lossReserveAmount": 90000000           → 422, no handler covers 9 crore
Body without claimNumber                               → 400, validation error
GET  http://localhost:8080/api/claims/CLM-001          → shows CH002
GET  http://localhost:8080/api/claims/XXX              → 404
```

Create a handler with several locations and policy numbers:

```
POST http://localhost:8080/api/handlers
{
  "handlerCode": "CH005",
  "handlerName": "Sneha Joshi",
  "locations": ["PUNE", "NASHIK", "NAGPUR"],
  "lob": "MOTOR",
  "minAmount": 1,
  "maxAmount": 500000,
  "policyNumbers": ["POL-7001", "POL-7002"],
  "insuredNames": [],
  "active": true
}
```

Update it. Send the full handler; these lists replace the old ones:

```
PUT http://localhost:8080/api/handlers/CH005
{
  "handlerCode": "CH005",
  "handlerName": "Sneha R Joshi",
  "locations": ["PUNE", "MUMBAI"],
  "lob": "MOTOR",
  "minAmount": 1,
  "maxAmount": 1000000,
  "policyNumbers": ["POL-7001", "POL-7002", "POL-7003"],
  "insuredNames": [],
  "active": true
}
```

Fetch it: `GET http://localhost:8080/api/handlers/CH005`

Add more values later (the "Add" button). `mappingType` is LOCATION, POLICY_NUMBER or INSURED_NAME:

```
POST http://localhost:8080/api/handlers/CH005/mappings
{ "mappingType": "POLICY_NUMBER", "values": ["POL-7003", "POL-7004"] }
```

Put a handler on half day:

```
POST http://localhost:8080/api/handlers/CH002/leaves
{ "leaveFrom": "2026-11-20T13:00:00", "leaveTo": "2026-11-20T18:00:00", "halfDay": true }
```

## Not done yet

- Product code, partner type and insured segment checks.
- Round robin when two handlers have the same workload. Currently the first one wins.
- The `Calculate Current Workload = N` branch, which should use pure round robin.
