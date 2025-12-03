package contactapp.service;

import contactapp.api.exception.DuplicateResourceException;
import contactapp.domain.Task;
import contactapp.persistence.store.InMemoryTaskStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-killing tests for TaskService validation guards.
 *
 * <p>These tests target VoidMethodCallMutator and boundary condition mutations
 * that survive in the H2/in-memory code path. Uses InMemoryTaskStore to ensure
 * tests run on both Windows and Linux CI environments without Testcontainers.
 *
 * <p><b>NOT tagged with "legacy-singleton"</b> - these tests run in the main pipeline.
 */
class TaskServiceValidationTest {

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(new InMemoryTaskStore());
        service.clearAllTasks();
    }

    // ==================== Mutation Testing: Validation Guards ====================

    /**
     * Kills VoidMethodCallMutator on deleteTask validation (line 191).
     * Ensures deleteTask throws IllegalArgumentException for null taskId, not NPE or other exception.
     */
    @Test
    void testDeleteTaskNullIdThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> service.deleteTask(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId must not be null or blank");
    }

    /**
     * Kills VoidMethodCallMutator on updateTask validation (line 216).
     * Ensures updateTask throws IllegalArgumentException for null taskId, not NPE.
     */
    @Test
    void testUpdateTaskNullIdThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> service.updateTask(null, "Name", "Desc"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId must not be null or blank");
    }

    /**
     * Kills VoidMethodCallMutator on getTaskById validation (line 419).
     * Ensures getTaskById throws IllegalArgumentException for null taskId, not NPE.
     */
    @Test
    void testGetTaskByIdNullIdThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> service.getTaskById(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId must not be null or blank");
    }

    /**
     * Kills VoidMethodCallMutator on clearAllTasks (line 574).
     * Verifies that clearAllTasks actually removes data from the store,
     * not just returns without calling store.deleteAll().
     */
    @Test
    void testClearAllTasksActuallyDeletesData() {
        // Add multiple tasks
        service.addTask(new Task("clear-01", "Task 1", "Desc 1"));
        service.addTask(new Task("clear-02", "Task 2", "Desc 2"));
        service.addTask(new Task("clear-03", "Task 3", "Desc 3"));

        // Verify tasks exist
        assertThat(service.getAllTasks()).hasSize(3);

        // Clear all tasks
        service.clearAllTasks();

        // Verify all tasks are gone - this kills the mutation that removes store.deleteAll()
        assertThat(service.getAllTasks()).isEmpty();

        // Also verify getTaskById returns empty for previously existing tasks
        assertThat(service.getTaskById("clear-01")).isEmpty();
        assertThat(service.getTaskById("clear-02")).isEmpty();
        assertThat(service.getTaskById("clear-03")).isEmpty();
    }

    /**
     * Kills BooleanTrueReturnValsMutator on addTask (line 168).
     * Although addTask can't return false (it throws on duplicate),
     * this test documents the expected behavior.
     */
    @Test
    void testAddTaskCannotReturnFalseOnlyThrowsOrReturnsTrue() {
        Task task = new Task("bool-test", "Task", "Desc");

        // First add should succeed
        boolean result = service.addTask(task);
        assertThat(result).isTrue();

        // Second add should throw, not return false
        Task duplicate = new Task("bool-test", "Another", "Desc");
        assertThatThrownBy(() -> service.addTask(duplicate))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("bool-test");
    }

    // ==================== Mutation Testing: Boundary Conditions ====================

    /**
     * Tests deleteTask with edge case: empty string after trim.
     * Ensures validation catches "" as blank, not just null.
     */
    @Test
    void testDeleteTaskEmptyStringAfterTrimThrows() {
        assertThatThrownBy(() -> service.deleteTask("   "))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId must not be null or blank");
    }

    /**
     * Tests updateTask with edge case: tab and newline characters.
     * Ensures validation rejects whitespace-only IDs.
     */
    @Test
    void testUpdateTaskWhitespaceOnlyIdThrows() {
        assertThatThrownBy(() -> service.updateTask("\t\n", "Name", "Desc"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId must not be null or blank");
    }

    /**
     * Tests getTaskById with edge case: ID that's blank after trim.
     */
    @Test
    void testGetTaskByIdBlankAfterTrimThrows() {
        assertThatThrownBy(() -> service.getTaskById("  \t  "))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId must not be null or blank");
    }
}
