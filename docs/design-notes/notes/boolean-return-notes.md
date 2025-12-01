# Boolean Return API Notes

(Related: [ADR-0035](../../adrs/ADR-0035-boolean-return-api.md))

## What problem this solves
Service methods can fail for different reasons:
- Duplicate ID (expected business case)
- ID not found (expected business case)
- Null input (bug - should never happen)

How do we communicate these to the caller?

## What the design is
**Return boolean for expected failures, throw for unexpected:**

```java
public boolean addContact(Contact contact) {
    if (contact == null) {
        throw new IllegalArgumentException();  // Bug! Crash fast.
    }
    if (store.existsById(contact.getId())) {
        return false;  // Expected - duplicate ID
    }
    store.save(contact);
    return true;  // Success
}
```

## Why not always throw exceptions?
```java
// This is verbose and slow:
try {
    service.addContact(contact);
} catch (DuplicateIdException e) {
    return ResponseEntity.status(409).body(...);
} catch (InvalidDataException e) {
    return ResponseEntity.badRequest().body(...);
}
```

vs.

```java
// This is clean:
if (!service.addContact(contact)) {
    return ResponseEntity.status(409).body(...);
}
```

## The rule
| Situation | What we do | Why |
|-----------|------------|-----|
| Duplicate ID | return false | Expected - controller returns 409 |
| Not found | return false | Expected - controller returns 404 |
| Null input | throw | Bug - should crash, fix the caller |
| Invalid data | throw | Bug - should crash, fix the caller |

## Simple explanation
"Booleans for expected failures (duplicate, not found). Exceptions for bugs (null, invalid). Keeps controller code clean."