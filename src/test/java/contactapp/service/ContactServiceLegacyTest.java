package contactapp.service;

import contactapp.domain.Contact;
import contactapp.persistence.store.ContactStore;
import contactapp.persistence.store.InMemoryContactStore;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the legacy {@link ContactService#getInstance()} path works outside Spring.
 */
class ContactServiceLegacyTest {

    @BeforeEach
    void resetSingleton() throws Exception {
        setInstance(null);
    }

    @AfterEach
    void cleanSingleton() throws Exception {
        setInstance(null);
    }

    @Test
    void coldStartReturnsInMemoryInstance() {
        ContactService legacy = ContactService.getInstance();
        assertThat(legacy).isNotNull();

        Contact contact = new Contact("900", "Legacy", "User", "1234567890", "Legacy Street");
        assertThat(legacy.addContact(contact)).isTrue();
        assertThat(legacy.getContactById("900")).isPresent();
    }

    @Test
    void repeatedLegacyCallsReturnSameInstance() {
        ContactService first = ContactService.getInstance();
        ContactService second = ContactService.getInstance();
        assertThat(first).isSameAs(second);
    }

    /**
     * When the Spring-managed bean starts after a legacy caller already populated
     * the in-memory store, {@link ContactService#registerInstance(ContactService)}
     * should migrate the defensive copies into the injected store and replace the singleton.
     */
    @Test
    void springBeanRegistrationCopiesLegacyDataIntoInjectedStore() throws Exception {
        ContactService legacy = createLegacyService();
        legacy.addContact(new Contact(
                "L-10",
                "Legacy",
                "User",
                "5554443322",
                "100 Legacy Way"));

        CapturingContactStore store = new CapturingContactStore();
        ContactService springBean = new ContactService(store);

        assertThat(store.findById("L-10"))
                .as("registerInstance should copy contacts from the temporary store")
                .isPresent();
        assertThat(ContactService.getInstance()).isSameAs(springBean);
    }

    private static void setInstance(final ContactService newInstance) throws Exception {
        Field instanceField = ContactService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, newInstance);
    }

    /**
     * Builds a legacy-mode service so tests control whether {@link #registerInstance(ContactService)}
     * copies data forward without mutating the global Spring context.
     */
    private static ContactService createLegacyService() throws Exception {
        Constructor<ContactService> constructor = ContactService.class
                .getDeclaredConstructor(ContactStore.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(new InMemoryContactStore(), true);
    }

    private static final class CapturingContactStore implements ContactStore {
        private final Map<String, Contact> database = new LinkedHashMap<>();

        @Override
        public boolean existsById(final String id) {
            return database.containsKey(id);
        }

        @Override
        public void save(final Contact aggregate) {
            database.put(aggregate.getContactId(), aggregate.copy());
        }

        @Override
        public Optional<Contact> findById(final String id) {
            return Optional.ofNullable(database.get(id)).map(Contact::copy);
        }

        @Override
        public List<Contact> findAll() {
            final List<Contact> contacts = new ArrayList<>();
            database.values().forEach(contact -> contacts.add(contact.copy()));
            return contacts;
        }

        @Override
        public boolean deleteById(final String id) {
            return database.remove(id) != null;
        }

        @Override
        public void deleteAll() {
            database.clear();
        }
    }
}
