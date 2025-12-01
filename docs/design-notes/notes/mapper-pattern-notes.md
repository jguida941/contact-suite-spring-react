# Mapper Pattern Notes

(Related: [ADR-0034](../../adrs/ADR-0034-mapper-pattern.md))

## What problem this solves
- Domain objects (`Contact`) have validation in constructors
- JPA entities need a no-arg constructor for Hibernate
- If we put @Entity on domain objects, Hibernate bypasses our validation when loading from DB
- We'd end up with potentially invalid domain objects

## What the design is
Keep them separate:

```
Contact (domain)          ContactEntity (JPA)         ContactMapper
- Validation in           - @Entity annotation        - Converts between
  constructor             - No-arg constructor          the two
- Business logic          - Just data holders         - Calls domain
- No JPA annotations      - Database columns            constructor
```

## The key insight
When mapper loads from database:
```java
public Contact toDomain(ContactEntity entity) {
    return new Contact(          // <-- Constructor runs!
        entity.getContactId(),   // Validation happens!
        entity.getFirstName(),
        ...
    );
}
```

Even data from the database goes through validation. If someone manually inserted bad data, we catch it immediately.

## Why not just one class?
```java
// This is problematic:
@Entity
public class Contact {
    public Contact(String id, ...) {
        validate(id);  // This runs when YOU create Contact
    }
    protected Contact() {}  // Hibernate uses THIS - skips validation!
}
```

Hibernate creates objects through the no-arg constructor, bypassing validation entirely.

## Simple explanation
"Domain stays pure, entity handles database stuff, mapper bridges them. Loading from DB still validates because mapper calls the domain constructor."