package contactapp.persistence.mapper;

import contactapp.domain.Appointment;
import contactapp.persistence.entity.AppointmentEntity;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link AppointmentMapper} converts between {@link java.util.Date} and {@link Instant}.
 */
class AppointmentMapperTest {

    private final AppointmentMapper mapper = new AppointmentMapper();

    @Test
    void toEntityConvertsDateToInstant() {
        Date futureDate = Date.from(Instant.now().plusSeconds(3_600));
        Appointment appointment = new Appointment("appt-1", futureDate, "Check-in");

        AppointmentEntity entity = mapper.toEntity(appointment);

        assertThat(entity.getAppointmentId()).isEqualTo("appt-1");
        assertThat(entity.getAppointmentDate()).isEqualTo(futureDate.toInstant());
        assertThat(entity.getDescription()).isEqualTo("Check-in");
    }

    @Test
    void toDomainConvertsInstantToDate() {
        Instant instant = Instant.now().plusSeconds(7_200);
        AppointmentEntity entity = new AppointmentEntity("appt-1", instant, "Check-in");

        Appointment appointment = mapper.toDomain(entity);

        assertThat(appointment.getAppointmentId()).isEqualTo("appt-1");
        assertThat(appointment.getAppointmentDate().toInstant())
                .isEqualTo(instant.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertThat(appointment.getDescription()).isEqualTo("Check-in");
    }

    /**
     * Null appointments (legacy fallback paths) should stay null instead of creating entities
     * with default-initialized fields.
     */
    @Test
    void toEntityReturnsNullWhenDomainIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    /**
     * Null JPA rows should not blow up when mapping back to the domain layer.
     */
    @Test
    void toDomainReturnsNullWhenEntityIsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    /**
     * Guard against null timestamps from JPA (should never happen, but the mapper
     * defends to keep downstream services predictable).
     */
    @Test
    void toDomainThrowsWhenEntityDateMissing() throws Exception {
        AppointmentEntity entity = new AppointmentEntity("appt-3", Instant.now(), "Missing instant");
        setField(entity, "appointmentDate", null);

        assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("appointmentDate");
    }

    private static void setField(final Object target, final String fieldName, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final IllegalAccessException | NoSuchFieldException ex) {
            throw new AssertionError(ex);
        }
    }
}
