package contactapp.persistence.mapper;

import contactapp.domain.Appointment;
import contactapp.persistence.entity.AppointmentEntity;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * Mapper bridging {@link Appointment} and {@link AppointmentEntity}.
 */
@Component
public class AppointmentMapper {

    public AppointmentEntity toEntity(final Appointment domain) {
        if (domain == null) {
            return null;
        }
        final Date appointmentDate = domain.getAppointmentDate();
        if (appointmentDate == null) {
            throw new IllegalArgumentException("appointmentDate must not be null");
        }
        final Instant instant = appointmentDate.toInstant();
        return new AppointmentEntity(
                domain.getAppointmentId(),
                instant,
                domain.getDescription());
    }

    public Appointment toDomain(final AppointmentEntity entity) {
        if (entity == null) {
            return null;
        }
        final Instant persistedInstant = entity.getAppointmentDate();
        if (persistedInstant == null) {
            throw new IllegalStateException("appointmentDate column must not be null");
        }
        final Date date = Date.from(persistedInstant);
        return new Appointment(
                entity.getAppointmentId(),
                date,
                entity.getDescription());
    }
}
