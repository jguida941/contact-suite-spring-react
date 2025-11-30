package contactapp.service;

import contactapp.config.ApplicationContextProvider;
import contactapp.domain.Contact;
import contactapp.domain.Validation;
import contactapp.persistence.store.ContactStore;
import contactapp.persistence.store.InMemoryContactStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for managing {@link Contact} instances.
 *
 * <p>Delegates persistence to a {@link ContactStore} so the same lifecycle
 * works for both Spring-managed JPA repositories and legacy callers that still
 * rely on {@link #getInstance()} before the application context starts.
 *
 * <h2>Design Decisions</h2>
 * <ul>
 *   <li>Singleton pattern retained for backward compatibility (legacy callers)</li>
 *   <li>{@code @Service} enables Spring DI in controllers (Phase 2+)</li>
 *   <li>{@code @Transactional} methods ensure repository operations run atomically</li>
 *   <li>{@link #clearAllContacts()} remains package-private for test isolation</li>
 * </ul>
 *
 * <h2>Why Not Final?</h2>
 * <p>This class was previously {@code final}. The modifier was removed because
 * Spring's {@code @Transactional} annotation uses CGLIB proxy subclassing,
 * which requires non-final classes for method interception.
 *
 * @see Contact
 * @see Validation
 */
@Service
@Transactional
public class ContactService {

    private static ContactService instance;

    private final ContactStore store;
    private final boolean legacyStore;

    /**
     * Primary constructor used by Spring to wire the JPA-backed store.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public ContactService(final ContactStore store) {
        this(store, false);
    }

    private ContactService(final ContactStore store, final boolean legacyStore) {
        this.store = store;
        this.legacyStore = legacyStore;
        registerInstance(this);
    }

    private static synchronized void registerInstance(final ContactService candidate) {
        if (instance != null && instance.legacyStore && !candidate.legacyStore) {
            instance.getAllContacts().forEach(candidate::addContact);
        }
        instance = candidate;
    }

    /**
     * Returns the global {@code ContactService} singleton instance.
     *
     * <p>The method is synchronized so that only one instance is created
     * even if multiple threads call it at the same time.
     *
     * <p>Note: When using Spring DI (e.g., in controllers), prefer constructor
     * injection over this method. This exists for backward compatibility.
     * Both access patterns share the same instance and backing store.
     *
     * <p>If called before Spring context initializes, lazily creates a service
     * backed by {@link InMemoryContactStore}. This preserves backward
     * compatibility for legacy non-Spring callers.
     *
     * @return the singleton {@code ContactService} instance
     */
    @SuppressFBWarnings(
            value = "MS_EXPOSE_REP",
            justification = "Singleton intentionally exposes shared instance for backward compatibility")
    public static synchronized ContactService getInstance() {
        if (instance != null) {
            return instance;
        }
        final ApplicationContext context = ApplicationContextProvider.getContext();
        if (context != null) {
            return context.getBean(ContactService.class);
        }
        return new ContactService(new InMemoryContactStore(), true);
    }

    /**
     * Adds a new contact to the store.
     *
     * <p>Uses database uniqueness constraint for atomic duplicate detection.
     * If a contact with the same ID already exists, the database throws
     * {@link DataIntegrityViolationException} which is caught and translated
     * to a {@code false} return value (controller returns 409 Conflict).
     *
     * @param contact the contact to add; must not be null
     * @return true if the contact was added, false if a duplicate ID exists
     * @throws IllegalArgumentException if contact is null
     */
    public boolean addContact(final Contact contact) {
        if (contact == null) {
            throw new IllegalArgumentException("contact must not be null");
        }
        final String contactId = contact.getContactId();
        if (contactId == null) {
            throw new IllegalArgumentException("contactId must not be null");
        }
        if (store.existsById(contactId)) {
            return false;
        }
        try {
            store.save(contact);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Duplicate ID - constraint violation from database
            return false;
        }
    }

    /**
     * Deletes a contact by id.
     *
     * <p>The provided id is validated and trimmed before removal so callers
     * can pass values like " 123 " and still target the stored entry for "123".
     *
     * @param contactId the id of the contact to remove; must not be null or blank
     * @return true if a contact was removed, false if no contact existed
     * @throws IllegalArgumentException if contactId is null or blank
     */
    public boolean deleteContact(final String contactId) {
        Validation.validateNotBlank(contactId, "contactId");
        return store.deleteById(contactId.trim());
    }

    /**
     * Updates an existing contact's mutable fields by id.
     *
     * @param contactId the id of the contact to update
     * @param firstName new first name
     * @param lastName  new last name
     * @param phone     new phone number
     * @param address   new address
     * @return true if the contact exists and was updated, false if no contact with that id exists
     * @throws IllegalArgumentException if any new field value is invalid
     */
    public boolean updateContact(
            final String contactId,
            final String firstName,
            final String lastName,
            final String phone,
            final String address) {
        Validation.validateNotBlank(contactId, "contactId");
        final String normalizedId = contactId.trim();

        final Optional<Contact> contact = store.findById(normalizedId);
        if (contact.isEmpty()) {
            return false;
        }
        final Contact existing = contact.get();
        existing.update(firstName, lastName, phone, address);
        store.save(existing);
        return true;
    }

    /**
     * Returns a read-only snapshot of the contact store.
     *
     * <p>Returns defensive copies of each Contact to prevent external mutation
     * of internal state. Modifications to the returned contacts do not affect
     * the contacts stored in the service.
     *
     * @return unmodifiable map of contact defensive copies
     */
    @Transactional(readOnly = true)
    public Map<String, Contact> getDatabase() {
        return store.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Contact::getContactId,
                        Contact::copy));
    }

    /**
     * Returns all contacts as a list of defensive copies.
     *
     * <p>Encapsulates the internal storage structure so controllers don't
     * need to access getDatabase() directly.
     *
     * @return list of contact defensive copies
     */
    @Transactional(readOnly = true)
    public List<Contact> getAllContacts() {
        return store.findAll().stream()
                .map(Contact::copy)
                .toList();
    }

    /**
     * Finds a contact by ID.
     *
     * <p>The ID is validated and trimmed before lookup so callers can pass
     * values like " 123 " and still find the contact stored as "123".
     *
     * @param contactId the contact ID to search for
     * @return Optional containing a defensive copy of the contact, or empty if not found
     * @throws IllegalArgumentException if contactId is null or blank
     */
    @Transactional(readOnly = true)
    public Optional<Contact> getContactById(final String contactId) {
        Validation.validateNotBlank(contactId, "contactId");
        return store.findById(contactId.trim()).map(Contact::copy);
    }

    void clearAllContacts() {
        store.deleteAll();
    }
}
