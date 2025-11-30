package contactapp.persistence.store;

import contactapp.domain.Appointment;
import contactapp.persistence.mapper.AppointmentMapper;
import contactapp.persistence.repository.AppointmentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * JPA-backed {@link AppointmentStore}.
 */
@Component
public class JpaAppointmentStore implements AppointmentStore {

    private final AppointmentRepository repository;
    private final AppointmentMapper mapper;

    public JpaAppointmentStore(
            final AppointmentRepository repository,
            final AppointmentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsById(final String id) {
        return repository.existsById(id);
    }

    @Override
    public void save(final Appointment aggregate) {
        repository.save(mapper.toEntity(aggregate));
    }

    @Override
    public Optional<Appointment> findById(final String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Appointment> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean deleteById(final String id) {
        return repository.findById(id)
                .map(entity -> {
                    repository.delete(entity);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }
}
