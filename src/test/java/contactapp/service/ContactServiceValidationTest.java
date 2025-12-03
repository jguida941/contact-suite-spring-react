package contactapp.service;

import contactapp.domain.Contact;
import contactapp.persistence.store.InMemoryContactStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-killing tests for ContactService validation guards.
 *
 * <p>Targets VoidMethodCallMutator and boundary condition mutations
 * that survive in the H2/in-memory code path.
 *
 * <p><b>NOT tagged with "legacy-singleton"</b> - runs in the main pipeline.
 */
class ContactServiceValidationTest {

    private ContactService service;

    @BeforeEach
    void setUp() {
        service = new ContactService(new InMemoryContactStore());
        service.clearAllContacts();
    }

    /**
     * Kills VoidMethodCallMutator on deleteContact validation (line 207).
     * Ensures deleteContact throws IllegalArgumentException for null contactId, not NPE.
     */
    @Test
    void testDeleteContactNullIdThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> service.deleteContact(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactId must not be null or blank");
    }

    /**
     * Boundary test: empty string after trim should be rejected.
     */
    @Test
    void testDeleteContactEmptyStringAfterTrimThrows() {
        assertThatThrownBy(() -> service.deleteContact("   "))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactId must not be null or blank");
    }

    /**
     * Boundary test: whitespace-only ID should be rejected.
     */
    @Test
    void testDeleteContactWhitespaceOnlyIdThrows() {
        assertThatThrownBy(() -> service.deleteContact("\t\n"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactId must not be null or blank");
    }

    /**
     * Verifies clearAllContacts actually removes data from the store.
     */
    @Test
    void testClearAllContactsActuallyDeletesData() {
        // Add multiple contacts
        service.addContact(new Contact("c1", "Alice", "Smith", "1234567890", "123 St"));
        service.addContact(new Contact("c2", "Bob", "Jones", "0987654321", "456 Ave"));

        // Verify contacts exist
        assertThat(service.getAllContacts()).hasSize(2);

        // Clear all contacts
        service.clearAllContacts();

        // Verify all contacts are gone
        assertThat(service.getAllContacts()).isEmpty();
        assertThat(service.getContactById("c1")).isEmpty();
        assertThat(service.getContactById("c2")).isEmpty();
    }
}
