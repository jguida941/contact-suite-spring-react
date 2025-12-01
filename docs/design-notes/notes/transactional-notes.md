# @Transactional Notes

(Related: [ADR-0033](../../adrs/ADR-0033-transaction-management.md))

## What problem this solves
Database operations can fail partway through. Without transactions:
```java
public boolean addContact(Contact contact) {
    if (store.existsById(id)) return false;  // Check
    // <-- Another request inserts same ID here!
    store.save(contact);  // Boom - duplicate key error
    return true;
}
```

## What @Transactional does
Wraps the method in a database transaction - all steps succeed or all fail together.

**Bank transfer analogy:**
```java
@Transactional
public void transfer(Account from, Account to, int amount) {
    from.withdraw(amount);  // Take money out
    to.deposit(amount);     // Put money in
}
```
- Without transaction: If deposit fails, money disappears (withdrawn but never deposited)
- With transaction: If deposit fails, withdrawal is rolled back - money stays put

## When we use it
| Method | @Transactional | Why |
|--------|----------------|-----|
| addContact | Yes | Check + save must be atomic |
| updateContact | Yes | Find + update must be atomic |
| deleteContact | Yes | Consistency |
| getAllContacts | Yes (readOnly) | Optimization - DB knows we're not writing |

## Simple explanation
"@Transactional makes database operations all-or-nothing. Like a bank transfer - money doesn't leave one account without arriving in the other."