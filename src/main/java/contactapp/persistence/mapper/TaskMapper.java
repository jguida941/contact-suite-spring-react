package contactapp.persistence.mapper;

import contactapp.domain.Task;
import contactapp.persistence.entity.TaskEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link Task} domain objects and {@link TaskEntity}.
 */
@Component
public class TaskMapper {

    public TaskEntity toEntity(final Task domain) {
        if (domain == null) {
            return null;
        }
        return new TaskEntity(
                domain.getTaskId(),
                domain.getName(),
                domain.getDescription());
    }

    public Task toDomain(final TaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Task(
                entity.getTaskId(),
                entity.getName(),
                entity.getDescription());
    }
}
