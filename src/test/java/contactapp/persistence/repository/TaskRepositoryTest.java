package contactapp.persistence.repository;

import contactapp.persistence.entity.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice tests for {@link TaskRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class TaskRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private TaskRepository repository;

    @Test
    void saveAndFindTask() {
        TaskEntity entity = new TaskEntity("task-101", "Repo Task", "Persist via repository");

        repository.saveAndFlush(entity);

        assertThat(repository.findById("task-101"))
                .isPresent()
                .get()
                .extracting(TaskEntity::getDescription)
                .isEqualTo("Persist via repository");
    }
}
