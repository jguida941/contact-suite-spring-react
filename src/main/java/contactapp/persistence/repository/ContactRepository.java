package contactapp.persistence.repository;

import contactapp.persistence.entity.ContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for persisting contacts using natural string IDs.
 */
@Repository
public interface ContactRepository extends JpaRepository<ContactEntity, String> {
}
