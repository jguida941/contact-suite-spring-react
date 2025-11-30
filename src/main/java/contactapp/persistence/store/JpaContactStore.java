package contactapp.persistence.store;

import contactapp.domain.Contact;
import contactapp.persistence.mapper.ContactMapper;
import contactapp.persistence.repository.ContactRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Primary {@link ContactStore} implementation backed by Spring Data JPA.
 */
@Component
public class JpaContactStore implements ContactStore {

    private final ContactRepository repository;
    private final ContactMapper mapper;

    public JpaContactStore(final ContactRepository repository, final ContactMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsById(final String id) {
        return repository.existsById(id);
    }

    @Override
    public void save(final Contact aggregate) {
        repository.save(mapper.toEntity(aggregate));
    }

    @Override
    public Optional<Contact> findById(final String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Contact> findAll() {
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
