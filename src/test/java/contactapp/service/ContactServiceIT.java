package contactapp.service;

import contactapp.domain.Contact;
import contactapp.persistence.entity.ContactEntity;
import contactapp.persistence.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full-stack ContactService tests against a real Postgres instance (Testcontainers).
 *
 * <p>Ensures Flyway migrations run, Spring Data repositories work, and DB constraints
 * behave the same as our domain validation.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers
class ContactServiceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ContactService contactService;

    @Autowired
    private ContactRepository contactRepository;

    @Test
    @Transactional
    void addContactPersistsRecord() {
        Contact contact = new Contact("it-100", "Integration", "Contact", "5555555555", "1 Integration Way");

        boolean added = contactService.addContact(contact);

        assertThat(added).isTrue();
        assertThat(contactRepository.findById("it-100")).isPresent();
    }

    @Test
    void databaseConstraintRejectsInvalidPhone() {
        ContactEntity entity = new ContactEntity("it-101", "Db", "Check", "invalid", "123 Somewhere");

        assertThatThrownBy(() -> contactRepository.saveAndFlush(entity))
                .as("phone column enforces digits-only constraint")
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
