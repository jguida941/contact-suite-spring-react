package contactapp.persistence.repository;

import contactapp.persistence.entity.ContactEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository slice tests for {@link ContactRepository} (H2 + Flyway schema).
 */

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class ContactRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private ContactRepository repository;

    @Test
    void savePersistsContact() {
        ContactEntity entity = new ContactEntity("repo-1", "Jane", "Doe", "5555555555", "Repo Street");

        repository.saveAndFlush(entity);

        assertThat(repository.findById("repo-1"))
                .isPresent()
                .get()
                .extracting(ContactEntity::getFirstName)
                .isEqualTo("Jane");
    }

    @Test
    void invalidPhoneFailsCheckConstraint() {
        ContactEntity entity = new ContactEntity("repo-2", "Jane", "Doe", "notdigits", "Repo Street");

        assertThatThrownBy(() -> repository.saveAndFlush(entity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
