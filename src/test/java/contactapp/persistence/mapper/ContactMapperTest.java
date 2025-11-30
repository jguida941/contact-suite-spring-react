package contactapp.persistence.mapper;

import contactapp.domain.Contact;
import contactapp.persistence.entity.ContactEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link ContactMapper} round-trips data and reuses domain validation.
 */
class ContactMapperTest {

    private final ContactMapper mapper = new ContactMapper();

    @Test
    void toEntityCopiesAllFields() {
        Contact contact = new Contact("1234567890", "Alice", "Smith", "1112223333", "123 Main St");

        ContactEntity entity = mapper.toEntity(contact);

        assertThat(entity.getContactId()).isEqualTo("1234567890");
        assertThat(entity.getFirstName()).isEqualTo("Alice");
        assertThat(entity.getLastName()).isEqualTo("Smith");
        assertThat(entity.getPhone()).isEqualTo("1112223333");
        assertThat(entity.getAddress()).isEqualTo("123 Main St");
    }

    @Test
    void toDomainReusesValidation() {
        ContactEntity entity = new ContactEntity("abc", "Bob", "Jones", "1234567890", "456 Elm");

        Contact contact = mapper.toDomain(entity);

        assertThat(contact.getContactId()).isEqualTo("abc");
        assertThat(contact.getFirstName()).isEqualTo("Bob");
    }

    /**
     * Null values can surface from legacy persistence fallbacks, so returning null keeps
     * callers from receiving phantom entities.
     */
    @Test
    void toEntityReturnsNullWhenDomainIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomainRejectsInvalidDatabaseData() {
        ContactEntity entity = new ContactEntity("abc", "Bob", "Jones", "invalid-phone", "456 Elm");

        assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phone");
    }

    /**
     * Mapper should simply return null when a repository hands us a null row.
     */
    @Test
    void toDomainReturnsNullWhenEntityIsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
