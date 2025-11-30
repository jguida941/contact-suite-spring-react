package contactapp.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures {@link TaskEntity} exposes predictable getters/setters for Hibernate and the mappers.
 */
class TaskEntityTest {

    @Test
    void constructorInitializesAllFields() {
        TaskEntity entity = new TaskEntity("T-5", "Write docs", "Explain persistence layer");

        assertThat(entity.getTaskId()).isEqualTo("T-5");
        assertThat(entity.getName()).isEqualTo("Write docs");
        assertThat(entity.getDescription()).isEqualTo("Explain persistence layer");
    }

    /**
     * Hibernate uses the protected constructor, so verify setters function when the entity
     * is populated via reflection.
     */
    @Test
    void settersUpdateStateWhenProxyMaterialized() {
        TaskEntity entity = new TaskEntity();

        entity.setTaskId("T-9");
        entity.setName("Refresh docs");
        entity.setDescription("Update README coverage notes");

        assertThat(entity.getTaskId()).isEqualTo("T-9");
        assertThat(entity.getName()).isEqualTo("Refresh docs");
        assertThat(entity.getDescription()).isEqualTo("Update README coverage notes");
    }
}
