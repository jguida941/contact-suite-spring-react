package contactapp.service;

import contactapp.domain.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the ContactService behavior.
 *
 * <p>Runs against the Spring context (H2 + Flyway) so the service exercises the real
 * persistence layer instead of the legacy in-memory map. Tests stay in the same package
 * to access package-private helpers like {@link ContactService#clearAllContacts()}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ContactServiceTest {

    @Autowired
    private ContactService service;

    /**
     * Clears the singleton map before each test run to keep scenarios isolated.
     */
    @BeforeEach
    void clearBeforeTest() {
        service.clearAllContacts();
    }

    /**
     * Ensures the Spring-managed bean and legacy singleton reference are the same instance.
     */
    @Test
    void testSingletonSharesStateWithSpringBean() {
        ContactService singleton = ContactService.getInstance();
        singleton.clearAllContacts();

        Contact legacyContact = new Contact(
                "legacy-100",
                "Legacy",
                "Singleton",
                "1112223333",
                "123 Legacy Lane"
        );

        boolean addedViaSingleton = singleton.addContact(legacyContact);

        assertThat(addedViaSingleton).isTrue();
        assertThat(service.getContactById("legacy-100")).isPresent();
    }

    /**
     * Confirms {@link ContactService#addContact(Contact)} inserts new contacts and stores them in the map.
     */
    @Test
    void testAddContact() {
        Contact contact = new Contact(
                "100",
                "Justin",
                "Guida",
                "1234567890",
                "7622 Main Street"
        );

        boolean added = service.addContact(contact);

        assertThat(added).isTrue();
        assertThat(service.getDatabase()).containsKey("100");
        Contact stored = service.getDatabase().get("100");
        assertThat(stored.getFirstName()).isEqualTo("Justin");
        assertThat(stored.getLastName()).isEqualTo("Guida");
        assertThat(stored.getPhone()).isEqualTo("1234567890");
        assertThat(stored.getAddress()).isEqualTo("7622 Main Street");
    }

    /**
     * Proves {@link ContactService#deleteContact(String)} removes existing contacts.
     */
    @Test
    void testDeleteContact() {
        ContactService contactService = this.service;
        Contact contact = new Contact(
                "100",
                "Justin",
                "Guida",
                "1234567890",
                "7622 Main Street"
        );

        boolean added = contactService.addContact(contact);

        assertThat(added).isTrue();
        assertThat(contactService.getDatabase()).containsKey("100");

        boolean deleted = contactService.deleteContact("100");

        assertThat(deleted).isTrue();
        assertThat(contactService.getDatabase()).doesNotContainKey("100");
    }

    /**
     * Ensures delete returns {@code false} when the contact ID is missing.
     */
    @Test
    void testDeleteMissingContactReturnsFalse() {
        ContactService contactService = this.service;
        assertThat(contactService.deleteContact("missing-id")).isFalse();
        assertThat(contactService.getDatabase()).isEmpty();
    }

    /**
     * Verifies {@link ContactService#updateContact} updates stored contacts.
     */
    @Test
    void testUpdateContact() {
        ContactService contactService = this.service;
        Contact contact = new Contact(
                "100",
                "Justin",
                "Guida",
                "1234567890",
                "7622 Main Street"
        );

        boolean added = contactService.addContact(contact);
        assertThat(added).isTrue();
        assertThat(contactService.getDatabase()).containsKey("100");

        boolean updated = contactService.updateContact(
                "100",
                "Sebastian",
                "Walker",
                "0987654321",
                "1234 Test Street"
        );

        assertThat(updated).isTrue();

        assertThat(contactService.getDatabase().get("100"))
                .hasFieldOrPropertyWithValue("firstName", "Sebastian")
                .hasFieldOrPropertyWithValue("lastName", "Walker")
                .hasFieldOrPropertyWithValue("phone", "0987654321")
                .hasFieldOrPropertyWithValue("address", "1234 Test Street");
    }

    /**
     * Confirms IDs are trimmed before lookups during updates.
     */
    @Test
    void testUpdateContactTrimsId() {
        ContactService service = this.service;
        Contact contact = new Contact(
                "200",
                "Justin",
                "Guida",
                "1234567890",
                "7622 Main Street"
        );

        service.addContact(contact);

        boolean updated = service.updateContact(
                " 200 ",
                "Sebastian",
                "Walker",
                "0987654321",
                "1234 Test Street"
        );

        assertThat(updated).isTrue();
        assertThat(service.getDatabase().get("200"))
                .hasFieldOrPropertyWithValue("firstName", "Sebastian")
                .hasFieldOrPropertyWithValue("lastName", "Walker")
                .hasFieldOrPropertyWithValue("phone", "0987654321")
                .hasFieldOrPropertyWithValue("address", "1234 Test Street");
    }

    /**
     * Ensures update throws when the ID is blank so validation mirrors delete().
     */
    @Test
    void testUpdateContactBlankIdThrows() {
        ContactService service = this.service;

        assertThatThrownBy(() -> service.updateContact(" ", "A", "B", "1234567890", "Somewhere"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("contactId must not be null or blank");
    }

    /**
     * Ensures duplicate contact IDs are rejected and the original remains stored.
     */
    @Test
    void testAddDuplicateContactFails() {
        ContactService service = this.service;
        Contact contact1 = new Contact("100", "Justin", "Guida", "1234567890", "7622 Main Street");
        Contact contact2 = new Contact("100", "Other", "Person", "1112223333", "Other Address");

        boolean firstAdd = service.addContact(contact1);
        boolean secondAdd = service.addContact(contact2);

        assertThat(firstAdd).isTrue();
        assertThat(secondAdd).isFalse();  // duplicate id rejected
        // Verify original data is still stored
        Contact stored = service.getDatabase().get("100");
        assertThat(stored.getFirstName()).isEqualTo("Justin");
        assertThat(stored.getLastName()).isEqualTo("Guida");
    }

    /**
     * Validates update returns {@code false} when the contact ID does not exist.
     */
    @Test
    void testUpdateMissingContactReturnsFalse() {
        ContactService service = this.service;

        boolean updated = service.updateContact(
                "does-not-exist",
                "Sebastian",
                "Guida",
                "0987654321",
                "1234 Test Street"
        );

        assertThat(updated).isFalse();
    }

    /**
     * Ensures delete throws when passed a blank ID.
     */
    @Test
    void testDeleteContactBlankIdThrows() {
        ContactService service = this.service;

        assertThatThrownBy(() -> service.deleteContact(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("contactId must not be null or blank");
    }

    /**
     * Verifies {@link ContactService#addContact(Contact)} guards against null input.
     */
    @Test
    void testAddContactNullThrows() {
        ContactService service = this.service;

        assertThatThrownBy(() -> service.addContact(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("contact must not be null");
    }

    /**
     * Ensures getDatabase returns defensive copies so external mutation cannot alter service state.
     */
    @Test
    void testGetDatabaseReturnsDefensiveCopies() {
        ContactService service = this.service;
        Contact contact = new Contact("500", "Original", "Name", "1234567890", "Original Address");
        service.addContact(contact);

        // Get a snapshot and mutate it
        Contact snapshot = service.getDatabase().get("500");
        snapshot.setFirstName("Mutated");
        snapshot.setLastName("Person");
        snapshot.setPhone("0987654321");
        snapshot.setAddress("Mutated Address");

        // Fetch a fresh snapshot and verify the service state is unchanged
        Contact freshSnapshot = service.getDatabase().get("500");
        assertThat(freshSnapshot.getFirstName()).isEqualTo("Original");
        assertThat(freshSnapshot.getLastName()).isEqualTo("Name");
        assertThat(freshSnapshot.getPhone()).isEqualTo("1234567890");
        assertThat(freshSnapshot.getAddress()).isEqualTo("Original Address");
    }

    // ==================== getContactById Tests ====================

    /**
     * Verifies getContactById returns the contact when it exists.
     */
    @Test
    void testGetContactByIdReturnsContact() {
        ContactService service = this.service;
        Contact contact = new Contact("600", "Test", "User", "1234567890", "Test Address");
        service.addContact(contact);

        var result = service.getContactById("600");

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("Test");
    }

    /**
     * Verifies getContactById returns empty when contact doesn't exist.
     */
    @Test
    void testGetContactByIdReturnsEmptyWhenNotFound() {
        ContactService service = this.service;

        var result = service.getContactById("nonexistent");

        assertThat(result).isEmpty();
    }

    /**
     * Verifies getContactById throws when ID is blank.
     */
    @Test
    void testGetContactByIdBlankIdThrows() {
        ContactService service = this.service;

        assertThatThrownBy(() -> service.getContactById(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("contactId must not be null or blank");
    }

    /**
     * Verifies getContactById trims the ID before lookup.
     */
    @Test
    void testGetContactByIdTrimsId() {
        ContactService service = this.service;
        Contact contact = new Contact("700", "Trimmed", "Test", "1234567890", "Test Address");
        service.addContact(contact);

        var result = service.getContactById(" 700 ");

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("Trimmed");
    }

    /**
     * Verifies getContactById returns a defensive copy.
     */
    @Test
    void testGetContactByIdReturnsDefensiveCopy() {
        ContactService service = this.service;
        Contact contact = new Contact("800", "Original", "Name", "1234567890", "Original Addr");
        service.addContact(contact);

        var result = service.getContactById("800");
        assertThat(result).isPresent();
        result.get().setFirstName("Mutated");

        var freshResult = service.getContactById("800");
        assertThat(freshResult).isPresent();
        assertThat(freshResult.get().getFirstName()).isEqualTo("Original");
    }

    // ==================== getAllContacts Tests ====================

    /**
     * Verifies getAllContacts returns empty list when no contacts exist.
     */
    @Test
    void testGetAllContactsReturnsEmptyList() {
        ContactService service = this.service;

        var result = service.getAllContacts();

        assertThat(result).isEmpty();
    }

    /**
     * Verifies getAllContacts returns all contacts.
     */
    @Test
    void testGetAllContactsReturnsAllContacts() {
        ContactService service = this.service;
        service.addContact(new Contact("901", "First", "User", "1111111111", "First Addr"));
        service.addContact(new Contact("902", "Second", "User", "2222222222", "Second Addr"));

        var result = service.getAllContacts();

        assertThat(result).hasSize(2);
    }
}
