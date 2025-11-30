package contactapp.service;

import contactapp.domain.Task;
import contactapp.persistence.entity.TaskEntity;
import contactapp.persistence.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full-stack TaskService tests against Postgres (Testcontainers).
 */
@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers
class TaskServiceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @Transactional
    void addTaskPersistsRecord() {
        Task task = new Task("it-task", "Write docs", "Integration test path");

        boolean added = taskService.addTask(task);

        assertThat(added).isTrue();
        assertThat(taskRepository.findById("it-task")).isPresent();
    }

    @Test
    void databaseRejectsNullDescription() {
        TaskEntity entity = new TaskEntity("it-task-null", "Name", null);

        assertThatThrownBy(() -> taskRepository.saveAndFlush(entity))
                .as("description column is NOT NULL")
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
