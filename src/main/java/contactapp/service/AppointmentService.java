package contactapp.service;

import contactapp.config.ApplicationContextProvider;
import contactapp.domain.Appointment;
import contactapp.domain.Validation;
import contactapp.persistence.store.AppointmentStore;
import contactapp.persistence.store.InMemoryAppointmentStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for managing {@link Appointment} instances.
 *
 * <p>Delegates persistence to {@link AppointmentStore} so Spring-managed JPA repositories
 * and legacy {@link #getInstance()} callers see the same behavior while
 * {@link #clearAllAppointments()} remains available for package-private test helpers.
 *
 * <h2>Why Not Final?</h2>
 * <p>This class was previously {@code final}. The modifier was removed because
 * Spring's {@code @Transactional} annotation uses CGLIB proxy subclassing,
 * which requires non-final classes for method interception.
 */
@Service
@Transactional
public class AppointmentService {

    private static AppointmentService instance;

    private final AppointmentStore store;
    private final boolean legacyStore;

    @org.springframework.beans.factory.annotation.Autowired
    public AppointmentService(final AppointmentStore store) {
        this(store, false);
    }

    private AppointmentService(final AppointmentStore store, final boolean legacyStore) {
        this.store = store;
        this.legacyStore = legacyStore;
        registerInstance(this);
    }

    private static synchronized void registerInstance(final AppointmentService candidate) {
        if (instance != null && instance.legacyStore && !candidate.legacyStore) {
            instance.getAllAppointments().forEach(candidate::addAppointment);
        }
        instance = candidate;
    }

    /**
     * Returns the shared AppointmentService singleton instance.
     *
     * <p>The method is synchronized so that concurrent callers cannot create
     * multiple singleton instances.
     *
     * <p>When running inside Spring, prefer constructor injection. This method
     * remains for backward compatibility with code that still calls it directly.
     *
     * <p>If called before Spring context initializes, lazily creates a service
     * backed by {@link InMemoryAppointmentStore}. This preserves backward
     * compatibility for legacy non-Spring callers.
     *
     * @return the singleton {@code AppointmentService} instance
     */
    @SuppressFBWarnings(
            value = "MS_EXPOSE_REP",
            justification = "Singleton intentionally exposes shared instance for backward compatibility")
    public static synchronized AppointmentService getInstance() {
        if (instance != null) {
            return instance;
        }
        final ApplicationContext context = ApplicationContextProvider.getContext();
        if (context != null) {
            return context.getBean(AppointmentService.class);
        }
        return new AppointmentService(new InMemoryAppointmentStore(), true);
    }

    /**
     * Adds an appointment if its id is not already present.
     *
     * <p>Uses database uniqueness constraint for atomic duplicate detection.
     * If an appointment with the same ID already exists, the database throws
     * {@link DataIntegrityViolationException} which is caught and translated
     * to a {@code false} return value (controller returns 409 Conflict).
     *
     * @param appointment the appointment to add; must not be null
     * @return true if the appointment was added, false if a duplicate ID exists
     * @throws IllegalArgumentException if appointment is null or has blank ID
     */
    public boolean addAppointment(final Appointment appointment) {
        if (appointment == null) {
            throw new IllegalArgumentException("appointment must not be null");
        }
        final String normalizedId = normalizeAndValidateId(appointment.getAppointmentId());
        if (store.existsById(normalizedId)) {
            return false;
        }
        try {
            store.save(appointment);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Duplicate ID - constraint violation from database
            return false;
        }
    }

    /**
     * Deletes an appointment by id.
     */
    public boolean deleteAppointment(final String appointmentId) {
        final String normalizedId = normalizeAndValidateId(appointmentId);
        return store.deleteById(normalizedId);
    }

    /**
     * Updates an existing appointment's mutable fields.
     */
    public boolean updateAppointment(
            final String appointmentId,
            final Date appointmentDate,
            final String description) {
        final String normalizedId = normalizeAndValidateId(appointmentId);
        final Optional<Appointment> appointment = store.findById(normalizedId);
        if (appointment.isEmpty()) {
            return false;
        }
        final Appointment existing = appointment.get();
        existing.update(appointmentDate, description);
        store.save(existing);
        return true;
    }

    /**
     * Returns an unmodifiable snapshot of the current store.
     */
    @Transactional(readOnly = true)
    public Map<String, Appointment> getDatabase() {
        return store.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Appointment::getAppointmentId,
                        Appointment::copy));
    }

    /**
     * Returns all appointments as a list of defensive copies.
     */
    @Transactional(readOnly = true)
    public List<Appointment> getAllAppointments() {
        return store.findAll().stream()
                .map(Appointment::copy)
                .toList();
    }

    /**
     * Finds an appointment by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Appointment> getAppointmentById(final String appointmentId) {
        final String normalizedId = normalizeAndValidateId(appointmentId);
        return store.findById(normalizedId).map(Appointment::copy);
    }

    void clearAllAppointments() {
        store.deleteAll();
    }

    private String normalizeAndValidateId(final String appointmentId) {
        Validation.validateNotBlank(appointmentId, "appointmentId");
        return appointmentId.trim();
    }
}
