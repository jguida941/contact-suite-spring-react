# Two-Layer Validation Notes

(Related: [ADR-0032](../../adrs/ADR-0032-two-layer-validation.md))

## What problem this solves
- API needs to return proper 400 errors with field names when input is bad
- Domain objects should NEVER be invalid - a Contact must always be valid once created
- If we only validate at API layer, someone could create invalid objects in tests or future code
- If we only validate at domain layer, API errors would be ugly 500s instead of clean 400s

## What the design is
We validate **twice**:

**Layer 1 - DTO (API boundary)**
```java
public record ContactRequest(
    @NotBlank @Size(max = 10) String id,
    @NotBlank @Size(max = 10) String firstName
) {}
```
- Uses Jakarta Bean Validation annotations
- Spring automatically returns 400 with field-specific errors
- Fast feedback before any business logic runs

**Layer 2 - Domain constructor**
```java
public Contact(String id, String firstName, ...) {
    Validation.validateLength(id, "contactId", 1, 10);
    // ...
}
```
- Domain is the safety net
- Can't construct an invalid Contact - ever
- Catches bugs in tests, migrations, future code paths

## How they stay in sync
DTOs import constants from domain:
```java
@Size(max = Validation.MAX_ID_LENGTH) String id
```
Change the constant once, both layers update.

## Simple explanation
"DTO validation gives good API errors. Domain validation guarantees objects are always valid. Belt and suspenders."