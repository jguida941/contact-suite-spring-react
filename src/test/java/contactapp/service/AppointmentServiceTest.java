package contactapp.service;

import contactapp.domain.Appointment;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link AppointmentService} behavior against the Spring context (H2 + Flyway).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AppointmentServiceTest {

    @Autowired
    private AppointmentService service;

    /**
     * Clears AppointmentService so each test operates on a clean in-memory store.
     */
    @BeforeEach
    void reset() {
        service.clearAllAppointments();
    }

    @Test
    void testSingletonSharesStateWithSpringBean() {
        AppointmentService singleton = AppointmentService.getInstance();
        singleton.clearAllAppointments();

        Date futureDate = futureDate(10);
        Appointment legacyAppointment = new Appointment("legacy-apt", futureDate, "Singleton path");
        boolean addedViaSingleton = singleton.addAppointment(legacyAppointment);

        assertThat(addedViaSingleton).isTrue();
        assertThat(service.getAppointmentById("legacy-apt")).isPresent();
    }

    /**
     * Confirms {@link AppointmentService#addAppointment(Appointment)} stores new appointments.
     */
    @Test
    void testAddAppointment() {
        AppointmentService service = this.service;

        Date futureDate = futureDate(30);
        Appointment appointment = new Appointment("200", futureDate, "Document Date");

        boolean added = service.addAppointment(appointment);

        assertThat(added).isTrue();
        assertThat(service.getDatabase()).containsKey("200");
        Appointment stored = service.getDatabase().get("200");
        assertThat(stored.getAppointmentDate()).isEqualTo(futureDate);
        assertThat(stored.getDescription()).isEqualTo("Document Date");
    }

    /**
     * Verifies delete removes stored appointments and returns true.
     */
    @Test
    void testDeleteAppointment() {
        AppointmentService service = this.service;

        Date futureDate = futureDate(30);
        Appointment appointment = new Appointment("700", futureDate, "Sample Appointment");
        service.addAppointment(appointment);

        boolean deleted = service.deleteAppointment("700");

        assertThat(deleted).isTrue();
        assertThat(service.getDatabase()).doesNotContainKey("700");
    }

    /**
     * Ensures {@link AppointmentService#updateAppointment} updates both fields.
     */
    @Test
    void testUpdateAppointment() {
        AppointmentService service = this.service;

        Date futureDate = futureDate(30);
        Appointment appointment = new Appointment("400", futureDate, "First Appointment");
        service.addAppointment(appointment);

        Date newDate = futureDate(60);

        boolean updated = service.updateAppointment("400", newDate, "Updated Appointment");

        Appointment updatedAppt = service.getDatabase().get("400");
        assertThat(updatedAppt.getAppointmentDate()).isEqualTo(newDate);
        assertThat(updatedAppt.getDescription()).isEqualTo("Updated Appointment");
        assertThat(updated).isTrue();
    }

    /**
     * Verifies duplicate IDs are rejected while the original entry remains.
     */
    @Test
    void testAddDuplicateAppointmentIdFails() {
        AppointmentService service = this.service;

        Date futureDate = futureDate(30);
        Appointment original = new Appointment("300", futureDate, "Document Date");
        Appointment duplicate = new Appointment("300", futureDate, "Duplicate Date");

        assertThat(service.addAppointment(original)).isTrue();
        assertThat(service.addAppointment(duplicate)).isFalse();
        Appointment stored = service.getDatabase().get("300");
        assertThat(stored.getAppointmentDate()).isEqualTo(futureDate);
        assertThat(stored.getDescription()).isEqualTo("Document Date");
    }

    /**
     * Ensures {@link AppointmentService#addAppointment(Appointment)} throws for null inputs.
     */
    @Test
    void testAddAppointmentNullThrows() {
        AppointmentService service = this.service;

        assertThatThrownBy(() -> service.addAppointment(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appointment must not be null");
    }

    /**
     * Proves IDs are validated even if a subclass returns blank IDs.
     */
    @Test
    void testAddAppointmentWithBlankIdThrows() throws Exception {
        AppointmentService service = this.service;
        Date future = futureDate(30);
        // Force a blank id via reflection to simulate corrupted input and hit the guard
        Appointment bad = new Appointment("tmp", future, "Desc");
        Field idField = Appointment.class.getDeclaredField("appointmentId");
        idField.setAccessible(true);
        idField.set(bad, " ");

        assertThatThrownBy(() -> service.addAppointment(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appointmentId must not be null or blank");
    }

    /**
     * Ensures delete enforces the not-blank ID rule.
     */
    @Test
    void testDeleteAppointmentBlankIdThrows() {
        AppointmentService service = this.service;

        assertThatThrownBy(() -> service.deleteAppointment(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appointmentId must not be null or blank");
    }

    /**
     * Verifies delete returns {@code false} when the ID does not exist.
     */
    @Test
    void testDeleteMissingAppointmentReturnsFalse() {
        AppointmentService service = this.service;

        assertThat(service.deleteAppointment("missing")).isFalse();
    }

    /**
     * Confirms {@link AppointmentService#clearAllAppointments()} empties the store.
     */
    @Test
    void testClearAllAppointmentsRemovesEntries() {
        AppointmentService service = this.service;

        Date futureDate = futureDate(30);
        Appointment appointment = new Appointment("500", futureDate, "Sample Appointment");
        service.addAppointment(appointment);

        service.clearAllAppointments();

        assertThat(service.getDatabase()).isEmpty();
    }

    /**
     * Ensures getDatabase returns defensive copies so external mutation cannot alter service state.
     */
    @Test
    void testGetDatabaseReturnsDefensiveCopies() {
        AppointmentService service = this.service;

        Date futureDate = futureDate(30);
        Appointment appointment = new Appointment("900", futureDate, "Immutable?");
        service.addAppointment(appointment);

        Appointment snapshot = service.getDatabase().get("900");
        snapshot.setDescription("Mutated outside service");

        Appointment freshSnapshot = service.getDatabase().get("900");
        assertThat(freshSnapshot.getDescription()).isEqualTo("Immutable?");
    }

    /**
     * Confirms update trims IDs before looking them up.
     */
    @Test
    void testUpdateAppointmentTrimsId() {
        AppointmentService service = this.service;

        Date futureDate = futureDate(45);
        Appointment appointment = new Appointment("600", futureDate, "Initial Description");
        service.addAppointment(appointment);

        boolean updated = service.updateAppointment(" 600 ", futureDate, "Updated Description");

        assertThat(updated).isTrue();
        Appointment updatedAppt = service.getDatabase().get("600");
        assertThat(updatedAppt.getDescription()).isEqualTo("Updated Description");
    }

    /**
     * Ensures update validates ID input before touching the map.
     */
    @Test
    void testUpdateAppointmentBlankIdThrows() {
        AppointmentService service = this.service;

        Date futureDate = futureDate(30);
        assertThatThrownBy(() -> service.updateAppointment(" ", futureDate, "Desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appointmentId must not be null or blank");
    }

    /**
     * Verifies update returns {@code false} when the appointment ID is missing.
     */
    @Test
    void testUpdateMissingAppointmentReturnsFalse() {
        AppointmentService service = this.service;

        Date futureDate = futureDate(30);
        assertThat(service.updateAppointment("missing", futureDate, "Desc")).isFalse();
    }

    /**
     * Helper to generate a zeroed future date relative to "now".
     */
    private static Date futureDate(int daysInFuture) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysInFuture);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
