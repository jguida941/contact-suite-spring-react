package contactapp.persistence.store;

import contactapp.domain.Appointment;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link InMemoryAppointmentStore} preserves defensive copies for legacy callers.
 */
class InMemoryAppointmentStoreTest {

    private final InMemoryAppointmentStore store = new InMemoryAppointmentStore();

    @BeforeEach
    void cleanStore() {
        store.deleteAll();
    }

    @Test
    void saveAndFindReturnCopiesOfAppointments() {
        Appointment appointment = new Appointment("A-1", futureDate(1), "Legacy appointment");
        store.save(appointment);

        assertThat(store.existsById("A-1")).isTrue();

        Appointment loaded = store.findById("A-1").orElseThrow();
        assertThat(loaded).isNotSameAs(appointment);

        Date mutated = loaded.getAppointmentDate();
        mutated.setTime(mutated.getTime() + 86_400_000);

        assertThat(store.findById("A-1").orElseThrow().getAppointmentDate())
                .isNotEqualTo(mutated);

        List<Appointment> snapshot = store.findAll();
        snapshot.get(0).setDescription("Mutated");
        assertThat(store.findById("A-1").orElseThrow().getDescription()).isEqualTo("Legacy appointment");
        assertThat(store.existsById("missing")).isFalse();
        // Optional.empty branch ensures missing IDs do not leak mutable state.
        assertThat(store.findById("missing")).isEmpty();
    }

    @Test
    void deleteOperationsClearEntries() {
        Appointment appointment = new Appointment("A-2", futureDate(2), "Remove me");
        store.save(appointment);

        assertThat(store.deleteById("A-2")).isTrue();
        assertThat(store.existsById("A-2")).isFalse();

        store.save(appointment);
        store.deleteAll();
        assertThat(store.findAll()).isEmpty();
        assertThat(store.deleteById("missing")).isFalse();
    }

    @Test
    void nullGuardsThrowExceptions() {
        assertThatThrownBy(() -> store.existsById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appointmentId");

        assertThatThrownBy(() -> store.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appointment must not be null");

        Appointment appointmentWithoutId = new Appointment("stub", futureDate(1), "desc");
        setField(appointmentWithoutId, "appointmentId", null);
        assertThatThrownBy(() -> store.save(appointmentWithoutId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appointmentId");

        assertThatThrownBy(() -> store.findById(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> store.deleteById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Date futureDate(final int daysAhead) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, daysAhead);
        return calendar.getTime();
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
