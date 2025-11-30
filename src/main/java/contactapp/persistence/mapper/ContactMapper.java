package contactapp.persistence.mapper;

import contactapp.domain.Contact;
import contactapp.persistence.entity.ContactEntity;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Contact} domain objects and {@link ContactEntity} rows.
 *
 * <p>{@link #toDomain(ContactEntity)} reuses the domain constructor so corrupted
 * database rows still go through {@link contactapp.domain.Validation}.
 */
@Component
public class ContactMapper {

    public ContactEntity toEntity(final Contact domain) {
        if (domain == null) {
            return null;
        }
        return new ContactEntity(
                domain.getContactId(),
                domain.getFirstName(),
                domain.getLastName(),
                domain.getPhone(),
                domain.getAddress());
    }

    public Contact toDomain(final ContactEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Contact(
                entity.getContactId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPhone(),
                entity.getAddress());
    }
}
