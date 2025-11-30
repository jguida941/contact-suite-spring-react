package contactapp.persistence.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates {@link AppointmentEntity} accessors so coverage includes the persistence layer.
 */
class AppointmentEntityTest {

    @Test
    void constructorStoresInstantFields() {
        Instant instant = Instant.parse("2030-01-01T00:00:00Z");
        AppointmentEntity entity = new AppointmentEntity("A-77", instant, "Future visit");

        assertThat(entity.getAppointmentId()).isEqualTo("A-77");
        assertThat(entity.getAppointmentDate()).isEqualTo(instant);
        assertThat(entity.getDescription()).isEqualTo("Future visit");
    }

    /**
     * JPA proxies rely on the protected constructor; setters must therefore populate
     * every field without additional validation.
     */
    @Test
    void settersWorkWhenEntityCreatedByHibernate() {
        AppointmentEntity entity = new AppointmentEntity();

        Instant instant = Instant.parse("2031-02-03T00:00:00Z");
        entity.setAppointmentId("A-88");
        entity.setAppointmentDate(instant);
        entity.setDescription("Proxy update");

        assertThat(entity.getAppointmentId()).isEqualTo("A-88");
        assertThat(entity.getAppointmentDate()).isEqualTo(instant);
        assertThat(entity.getDescription()).isEqualTo("Proxy update");
    }
}
