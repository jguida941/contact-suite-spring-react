package contactapp.persistence.store;

import contactapp.domain.Contact;

/**
 * Marker interface tying {@link Contact} aggregates to the generic {@link DomainDataStore}.
 */
public interface ContactStore extends DomainDataStore<Contact> {
}
