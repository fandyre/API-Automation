# Petstore API Tests

API tests for [Swagger Petstore v2](https://petstore.swagger.io/v2)

**5 positive + 5 negative test cases**, plain JUnit 5 + REST Assured.

---
# Video
https://drive.google.com/file/d/1_evC6h62qzhScv7vC6jbh3hDu6hHoGoF/view?usp=sharing

---
## What We Test

### Pet (`/pet`)
**Positive**
- `POST /pet` — does a new pet come back with the fields we submitted and a generated id?
- `GET /pet/{petId}` — does reading it back return the same record?
- `PUT /pet` — do name and status update while the id stays put?
- `GET /pet/findByStatus` — does a status filter return *only* pets with that status? (runs for available, pending, sold)

**Negative**
- What happens with a pet id that does not exist?
- What happens with a non-numeric pet id?
- What happens when the request body is not valid JSON?
- What happens with a status value that is not in the enum?

### Store (`/store/order`)

**Positive**
- `POST /store/order` — can we order a pet we created, and read the order back with the right `petId`?

**Negative**
- What happens with an order id that does not exist?

### Every test also checks
- HTTP status code
- Response matches the expected JSON schema
- Error responses carry the documented `code` / `type` / `message` envelope
---
## Project Structure

```
src/
├── main/java/
│   ├── Api.java          ← Makes API calls (endpoints + fluent Request builder)
│   └── Json.java         ← Reads JSON data & validates schemas
│
└── test/
    ├── java/
    │   ├── BaseTest.java     ← Shared assertions, request helpers, cleanup
    │   ├── CaseSummaryListener.java  ← Report formatting 
    │   ├── PetTest.java      ← 4 positive + 4 negative
    │   └── StoreTest.java    ← 1 positive + 1 negative
    │
    └── resources/
        ├── data/             ← Test data (statuses, bad ids, malformed bodies)
        │   ├── common.json
        │   └── pets.json
        └── schemas/          ← JSON schemas
            ├── pet-schema.json
            ├── pet-list-schema.json
            ├── order-schema.json
            └── error-schema.json
```
Test data lives in JSON, not in the test code, so adding another bad status or another unknown id is a one-line data change rather than a code change.

---

# Run
```bash
mvn clean test                    # run everything
mvn test -Dtest=PetTest           # pet endpoint only
mvn test -Dtest=StoreTest         # store endpoint only
```
---

# Report
Report can be seen on (`target/case-report/index.html`) and you can open it on any browser
