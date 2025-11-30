package contactapp.persistence.repository;

import contactapp.persistence.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link TaskEntity}.
 */
@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, String> {
}
