package contactapp.persistence.store;

import contactapp.domain.Contact;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory store used when {@link contactapp.service.ContactService#getInstance()}
 * is accessed before Spring initializes the JPA infrastructure.
 */
public class InMemoryContactStore implements ContactStore {

    private final Map<String, Contact> database = new ConcurrentHashMap<>();

    @Override
    public boolean existsById(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("contactId must not be null");
        }
        return database.containsKey(id);
    }

    @Override
    public void save(final Contact aggregate) {
        if (aggregate == null) {
            throw new IllegalArgumentException("contact aggregate must not be null");
        }
        final String contactId = aggregate.getContactId();
        if (contactId == null) {
            throw new IllegalArgumentException("contactId must not be null");
        }
        final Contact copy = aggregate.copy();
        if (copy == null) {
            throw new IllegalStateException("contact copy must not be null");
        }
        database.put(contactId, copy);
    }

    @Override
    public Optional<Contact> findById(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("contactId must not be null");
        }
        final Contact contact = database.get(id);
        return contact == null ? Optional.empty() : Optional.of(contact.copy());
    }

    @Override
    public List<Contact> findAll() {
        final List<Contact> contacts = new ArrayList<>();
        database.values().forEach(contact -> contacts.add(contact.copy()));
        return contacts;
    }

    @Override
    public boolean deleteById(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("contactId must not be null");
        }
        return database.remove(id) != null;
    }

    @Override
    public void deleteAll() {
        database.clear();
    }
}
