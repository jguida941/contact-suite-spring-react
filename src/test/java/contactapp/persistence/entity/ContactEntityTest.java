package contactapp.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the mutable {@link ContactEntity} accessor surface so JaCoCo counts the JPA layer.
 */
class ContactEntityTest {

    @Test
    void constructorAndGettersExposeFields() {
        ContactEntity entity = new ContactEntity("C-100", "Alice", "Smith", "1112223333", "123 Main");

        assertThat(entity.getContactId()).isEqualTo("C-100");
        assertThat(entity.getFirstName()).isEqualTo("Alice");
        assertThat(entity.getLastName()).isEqualTo("Smith");
        assertThat(entity.getPhone()).isEqualTo("1112223333");
        assertThat(entity.getAddress()).isEqualTo("123 Main");
    }

    /**
     * The protected no-args constructor exists strictly for Hibernate; this test ensures
     * setters keep working even when an empty proxy is materialized.
     */
    @Test
    void jpaConstructorAllowsPropertyMutation() {
        ContactEntity entity = new ContactEntity();

        entity.setFirstName("Jane");
        entity.setLastName("Doe");
        entity.setPhone("9998887777");
        entity.setAddress("456 Elm");
        entity.setFirstName("Janet"); // ensure setters overwrite values

        assertThat(entity.getFirstName()).isEqualTo("Janet");
        assertThat(entity.getLastName()).isEqualTo("Doe");
        assertThat(entity.getPhone()).isEqualTo("9998887777");
        assertThat(entity.getAddress()).isEqualTo("456 Elm");
    }
}
