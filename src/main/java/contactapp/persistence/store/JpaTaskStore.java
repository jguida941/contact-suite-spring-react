package contactapp.persistence.store;

import contactapp.domain.Task;
import contactapp.persistence.mapper.TaskMapper;
import contactapp.persistence.repository.TaskRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * JPA-backed {@link TaskStore}.
 */
@Component
public class JpaTaskStore implements TaskStore {

    private final TaskRepository repository;
    private final TaskMapper mapper;

    public JpaTaskStore(final TaskRepository repository, final TaskMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsById(final String id) {
        return repository.existsById(id);
    }

    @Override
    public void save(final Task aggregate) {
        repository.save(mapper.toEntity(aggregate));
    }

    @Override
    public Optional<Task> findById(final String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Task> findAll() {
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
