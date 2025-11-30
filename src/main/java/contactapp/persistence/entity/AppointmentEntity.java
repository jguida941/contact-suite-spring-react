package contactapp.persistence.entity;

import contactapp.domain.Validation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Persistence representation for {@link contactapp.domain.Appointment}.
 *
 * <p>Stores timestamps as {@link Instant} so they map cleanly to
 * {@code TIMESTAMP WITH TIME ZONE}.
 */
@Entity
@Table(name = "appointments")
public class AppointmentEntity {

    @Id
    @Column(name = "appointment_id", length = Validation.MAX_ID_LENGTH, nullable = false)
    private String appointmentId;

    @Column(name = "appointment_date", nullable = false)
    private Instant appointmentDate;

    @Column(name = "description", length = Validation.MAX_DESCRIPTION_LENGTH, nullable = false)
    private String description;

    protected AppointmentEntity() {
        // JPA only
    }

    public AppointmentEntity(
            final String appointmentId,
            final Instant appointmentDate,
            final String description) {
        this.appointmentId = appointmentId;
        this.appointmentDate = appointmentDate;
        this.description = description;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(final String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Instant getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(final Instant appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
