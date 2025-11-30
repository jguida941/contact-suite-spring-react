package contactapp.persistence.store;

import contactapp.domain.Task;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ensures the legacy {@link InMemoryTaskStore} keeps defensive-copy semantics.
 */
class InMemoryTaskStoreTest {

    private final InMemoryTaskStore store = new InMemoryTaskStore();

    @BeforeEach
    void cleanStore() {
        store.deleteAll();
    }

    @Test
    void saveAndFindProduceIndependentCopies() {
        Task task = new Task("T-1", "Legacy Task", "Legacy Description");
        store.save(task);

        assertThat(store.existsById("T-1")).isTrue();

        Task loaded = store.findById("T-1").orElseThrow();
        assertThat(loaded).isNotSameAs(task);

        loaded.setName("Mutated");
        assertThat(store.findById("T-1").orElseThrow().getName()).isEqualTo("Legacy Task");

        List<Task> snapshot = store.findAll();
        snapshot.get(0).setDescription("Changed");
        assertThat(store.findById("T-1").orElseThrow().getDescription()).isEqualTo("Legacy Description");
        assertThat(store.existsById("missing")).isFalse();
        // Null branch in findById should return Optional.empty for missing IDs.
        assertThat(store.findById("missing")).isEmpty();
    }

    @Test
    void deleteByIdAndDeleteAllClearEntries() {
        Task task = new Task("T-2", "Delete Task", "Remove me");
        store.save(task);

        assertThat(store.deleteById("T-2")).isTrue();
        assertThat(store.existsById("T-2")).isFalse();

        store.save(task);
        store.deleteAll();
        assertThat(store.findAll()).isEmpty();
        assertThat(store.deleteById("missing")).isFalse();
    }

    @Test
    void nullGuardsThrowExceptions() {
        assertThatThrownBy(() -> store.existsById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId");

        assertThatThrownBy(() -> store.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("task aggregate");

        Task taskWithoutId = new Task("stub", "Name", "Desc");
        setField(taskWithoutId, "taskId", null);
        assertThatThrownBy(() -> store.save(taskWithoutId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId");

        assertThatThrownBy(() -> store.findById(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> store.deleteById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void setField(final Object target, final String fieldName, final Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final IllegalAccessException | NoSuchFieldException ex) {
            throw new AssertionError(ex);
        }
    }
}
