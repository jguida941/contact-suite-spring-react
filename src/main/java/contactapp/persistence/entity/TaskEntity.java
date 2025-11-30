package contactapp.persistence.entity;

import contactapp.domain.Validation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Hibernate entity for {@link contactapp.domain.Task} persistence.
 */
@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    @Column(name = "task_id", length = Validation.MAX_ID_LENGTH, nullable = false)
    private String taskId;

    @Column(name = "name", length = Validation.MAX_TASK_NAME_LENGTH, nullable = false)
    private String name;

    @Column(name = "description", length = Validation.MAX_DESCRIPTION_LENGTH, nullable = false)
    private String description;

    protected TaskEntity() {
        // JPA only
    }

    public TaskEntity(
            final String taskId,
            final String name,
            final String description) {
        this.taskId = taskId;
        this.name = name;
        this.description = description;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
