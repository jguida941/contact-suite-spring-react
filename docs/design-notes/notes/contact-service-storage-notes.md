# ContactService singleton and storage

> **Note**: This document describes the legacy in-memory `ConcurrentHashMap` implementation. The current production system uses JPA persistence with `ContactStore` abstraction. See [ADR-0024](../../adrs/ADR-0024-persistence-implementation.md) for the current architecture.

(Related: [ADR-0002](../../adrs/ADR-0002-contact-service-storage.md), [ContactService.java](../../../src/main/java/contactapp/service/ContactService.java))

File: docs/design-notes/notes/contact-service-storage-notes.md

## What problem this solves
- The app needs one central list of contacts so every caller sees the same state.
- A singleton keeps the store in one place instead of forcing callers to pass maps around.

## What the design is
- `ContactService` is a singleton accessed via `getInstance()` or Spring DI.
- Uses a static `ConcurrentHashMap<String, Contact>` so both access paths share the same data.
- Service API:
    - `addContact(Contact)` checks `existsById()` then calls `save()` (upsert via `put`), returning `boolean`.
    - `deleteContact(String id)` validates the id then calls `remove`, returning `boolean`.
    - `updateContact(String id, ...)` retrieves the contact, delegates to `Contact.update(...)` for validation, then calls `save()` to persist.
    - `getDatabase()` returns defensive copies of each Contact (via `copy()`) in an unmodifiable map, preventing external mutation of internal state.
    - `clearAllContacts()` wipes the store so tests can reset between runs.

## Why this design
### Why `ConcurrentHashMap`
- Acts like a normal `HashMap` but allows concurrent access without explicit locking for get/put/remove.
- O(1) average time for add/update/delete operations.

### Why `put()` with service-level uniqueness check
- The store's `save()` method uses `put()` for upsert semantics (create or update).
- Uniqueness for ADD operations is enforced at the service layer via `existsById()` before calling `save()`.
- This separation allows the same `save()` method to handle both creates and updates, which is required when the service calls `save()` after modifying an existing entity.

### Why service-layer orchestration for updates
- The service retrieves the entity, applies changes via domain methods, then persists via `save()`.
- This pattern matches the JPA store behavior where `save()` handles both new and existing entities.
- Keeps validation logic centralized in the domain layer (`Contact.update()`).

### Why defensive copies and `clearAllContacts`
- If `getDatabase()` returned the real `database` or just a shallow `Map.copyOf`, a caller could mutate the Contact objects and bypass the service API rules.
  That is why we return defensive copies of each Contact (via `Contact.copy()`) in an unmodifiable map.
  Callers can read entries but any mutation only affects the copy, not the internal state; any attempt to `put` or `clear` throws `UnsupportedOperationException`.
- `clearAllContacts()` exists because the service is a singleton shared across tests.
  Without it, a contact added in one JUnit test would still be present in the next test.
  Tests call `ContactService.getInstance().clearAllContacts()` (usually in `@BeforeEach`) to guarantee a clean database every time.
  The method is **package-private** to prevent accidental calls from production code outside the `contactapp` package while still allowing test access (tests reside in the same package).

### Why booleans for service methods
- `add/delete/update` returning `boolean` keeps the API simple: success vs duplicate/missing.
- Tests assert on those booleans to cover the duplicate/missing branches without needing exceptions for normal control flow.
