package contactapp.persistence.mapper;

import contactapp.domain.Task;
import contactapp.persistence.entity.TaskEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures {@link TaskMapper} copies fields between domain and entity objects.
 */
class TaskMapperTest {

    private final TaskMapper mapper = new TaskMapper();

    @Test
    void toEntityCopiesAllFields() {
        Task task = new Task("task-1", "Write docs", "Document persistence layer");

        TaskEntity entity = mapper.toEntity(task);

        assertThat(entity.getTaskId()).isEqualTo("task-1");
        assertThat(entity.getName()).isEqualTo("Write docs");
        assertThat(entity.getDescription()).isEqualTo("Document persistence layer");
    }

    /**
     * Null inputs should short-circuit to null so callers only get defensive copies
     * when a real domain object exists.
     */
    @Test
    void toEntityReturnsNullWhenDomainIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomainRoundTripsEntity() {
        TaskEntity entity = new TaskEntity("task-1", "Write docs", "Document persistence layer");

        Task task = mapper.toDomain(entity);

        assertThat(task.getTaskId()).isEqualTo("task-1");
        assertThat(task.getName()).isEqualTo("Write docs");
        assertThat(task.getDescription()).isEqualTo("Document persistence layer");
    }

    /**
     * JPA repositories can surface null rows during delete-or-insert flows, so make sure
     * the mapper handles those gracefully instead of throwing.
     */
    @Test
    void toDomainReturnsNullWhenEntityIsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
