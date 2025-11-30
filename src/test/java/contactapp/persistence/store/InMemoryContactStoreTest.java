package contactapp.persistence.store;

import contactapp.domain.Contact;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the legacy {@link InMemoryContactStore} so mutation testing can
 * verify every branch even though production code uses JPA stores.
 */
class InMemoryContactStoreTest {

    private final InMemoryContactStore store = new InMemoryContactStore();

    @BeforeEach
    void cleanStore() {
        store.deleteAll();
    }

    @Test
    void saveAndFindReturnDefensiveCopies() {
        Contact contact = new Contact("C-1", "Legacy", "Contact", "1112223333", "1 Legacy Way");

        store.save(contact);

        assertThat(store.existsById("C-1")).isTrue();
        Contact loaded = store.findById("C-1").orElseThrow();
        assertThat(loaded).isNotSameAs(contact);

        loaded.setFirstName("Changed");
        assertThat(store.findById("C-1").orElseThrow().getFirstName()).isEqualTo("Legacy");

        List<Contact> snapshot = store.findAll();
        snapshot.get(0).setLastName("Mutated");
        assertThat(store.findById("C-1").orElseThrow().getLastName()).isEqualTo("Contact");
        assertThat(store.existsById("missing")).isFalse();
        // Missing IDs should return Optional.empty so the null branch in findById stays covered.
        assertThat(store.findById("missing")).isEmpty();
    }

    @Test
    void deleteOperationsRemoveEntries() {
        Contact contact = new Contact("C-2", "Delete", "Me", "4445556666", "2 Legacy Way");
        store.save(contact);

        assertThat(store.deleteById("C-2")).isTrue();
        assertThat(store.existsById("C-2")).isFalse();

        store.save(contact);
        store.deleteAll();
        assertThat(store.findAll()).isEmpty();
        assertThat(store.deleteById("missing")).isFalse();
    }

    @Test
    void nullGuardsThrowExceptions() {
        assertThatThrownBy(() -> store.existsById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactId");

        assertThatThrownBy(() -> store.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contact aggregate");

        Contact contactWithoutId = new Contact("stub", "A", "B", "1234567890", "Street");
        setField(contactWithoutId, "contactId", null);
        assertThatThrownBy(() -> store.save(contactWithoutId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactId");

        assertThatThrownBy(() -> store.findById(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> store.deleteById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void setField(final Object target, final String fieldName, final Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final IllegalAccessException | NoSuchFieldException ex) {
            throw new AssertionError(ex);
        }
    }
}
