package contactapp.persistence.repository;

import contactapp.persistence.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link AppointmentEntity}.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, String> {
}
