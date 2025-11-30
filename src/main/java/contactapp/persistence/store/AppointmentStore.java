package contactapp.persistence.store;

import contactapp.domain.Appointment;

/**
 * Appointment-specific persistence abstraction layered over {@link DomainDataStore}.
 */
public interface AppointmentStore extends DomainDataStore<Appointment> {
}
