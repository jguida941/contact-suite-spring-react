package contactapp.service;

import contactapp.config.ApplicationContextProvider;
import contactapp.domain.Task;
import contactapp.domain.Validation;
import contactapp.persistence.store.InMemoryTaskStore;
import contactapp.persistence.store.TaskStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for creating, updating, and deleting {@link Task} objects.
 *
 * <p>Persistence is delegated to {@link TaskStore} so Spring-managed JPA repositories
 * and legacy {@link #getInstance()} callers share the same behavior while
 * {@link #clearAllTasks()} stays package-private for test isolation.
 *
 * <h2>Why Not Final?</h2>
 * <p>This class was previously {@code final}. The modifier was removed because
 * Spring's {@code @Transactional} annotation uses CGLIB proxy subclassing,
 * which requires non-final classes for method interception.
 */
@Service
@Transactional
public class TaskService {

    private static TaskService instance;

    private final TaskStore store;
    private final boolean legacyStore;

    @org.springframework.beans.factory.annotation.Autowired
    public TaskService(final TaskStore store) {
        this(store, false);
    }

    private TaskService(final TaskStore store, final boolean legacyStore) {
        this.store = store;
        this.legacyStore = legacyStore;
        registerInstance(this);
    }

    private static synchronized void registerInstance(final TaskService candidate) {
        if (instance != null && instance.legacyStore && !candidate.legacyStore) {
            instance.getAllTasks().forEach(candidate::addTask);
        }
        instance = candidate;
    }

    /**
     * Returns the single shared TaskService instance.
     *
     * <p>Thread safety: The method is synchronized so that, if multiple threads
     * call it at the same time, only one will create the instance.
     *
     * <p>Note: When using Spring DI (e.g., in controllers), prefer constructor
     * injection over this method. This exists for backward compatibility.
     * Both access patterns share the same instance and backing store.
     *
     * <p>If called before Spring context initializes, lazily creates a service
     * backed by {@link InMemoryTaskStore}. This preserves backward
     * compatibility for legacy non-Spring callers.
     *
     * @return the singleton TaskService instance
     */
    @SuppressFBWarnings(
            value = "MS_EXPOSE_REP",
            justification = "Singleton intentionally exposes shared instance for backward compatibility")
    public static synchronized TaskService getInstance() {
        if (instance != null) {
            return instance;
        }
        final ApplicationContext context = ApplicationContextProvider.getContext();
        if (context != null) {
            return context.getBean(TaskService.class);
        }
        return new TaskService(new InMemoryTaskStore(), true);
    }

    /**
     * Adds a new task to the persistent store.
     *
     * <p>Uses database uniqueness constraint for atomic duplicate detection.
     * If a task with the same ID already exists, the database throws
     * {@link DataIntegrityViolationException} which is caught and translated
     * to a {@code false} return value (controller returns 409 Conflict).
     *
     * @param task the task to add; must not be null
     * @return true if the task was added, false if a duplicate ID exists
     * @throws IllegalArgumentException if task is null
     */
    public boolean addTask(final Task task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        final String taskId = task.getTaskId();
        if (taskId == null) {
            throw new IllegalArgumentException("taskId must not be null");
        }
        if (store.existsById(taskId)) {
            return false;
        }
        try {
            store.save(task);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Duplicate ID - constraint violation from database
            return false;
        }
    }

    /**
     * Deletes a task by id.
     *
     * <p>The id is validated and trimmed before removal so callers can pass
     * whitespace and still reference the stored entry.
     *
     * @param taskId id to remove; must not be blank
     * @return true if removed, false otherwise
     * @throws IllegalArgumentException if taskId is null or blank
     */
    public boolean deleteTask(final String taskId) {
        Validation.validateNotBlank(taskId, "taskId");
        return store.deleteById(taskId.trim());
    }

    /**
     * Updates the name and description of an existing task.
     */
    public boolean updateTask(
            final String taskId,
            final String newName,
            final String description) {
        Validation.validateNotBlank(taskId, "taskId");
        final String normalizedId = taskId.trim();

        final Optional<Task> task = store.findById(normalizedId);
        if (task.isEmpty()) {
            return false;
        }
        final Task existing = task.get();
        existing.update(newName, description);
        store.save(existing);
        return true;
    }

    /**
     * Returns an unmodifiable snapshot of the current task store.
     *
     * <p>Returns defensive copies of each Task to prevent external mutation
     * of internal state.
     *
     * @return unmodifiable map of defensive task copies
     */
    @Transactional(readOnly = true)
    public Map<String, Task> getDatabase() {
        return store.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Task::getTaskId,
                        Task::copy));
    }

    /**
     * Returns all tasks as a list of defensive copies.
     *
     * <p>Encapsulates the internal storage structure so controllers don't
     * need to access getDatabase() directly.
     *
     * @return list of task defensive copies
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        return store.findAll().stream()
                .map(Task::copy)
                .toList();
    }

    /**
     * Finds a task by ID.
     *
     * <p>The ID is validated and trimmed before lookup so callers can pass
     * values like " 123 " and still find the task stored as "123".
     *
     * @param taskId the task ID to search for
     * @return Optional containing a defensive copy of the task, or empty if not found
     * @throws IllegalArgumentException if taskId is null or blank
     */
    @Transactional(readOnly = true)
    public Optional<Task> getTaskById(final String taskId) {
        Validation.validateNotBlank(taskId, "taskId");
        return store.findById(taskId.trim()).map(Task::copy);
    }

    void clearAllTasks() {
        store.deleteAll();
    }
}
