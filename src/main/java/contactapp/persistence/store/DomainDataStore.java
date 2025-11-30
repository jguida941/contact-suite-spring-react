package contactapp.persistence.store;

import java.util.List;
import java.util.Optional;

/**
 * Generic persistence contract for domain aggregates.
 *
 * <p>Services depend on this interface rather than Spring Data directly so we can
 * provide multiple implementations (JPA-backed for normal operation and in-memory
 * fallbacks for {@code getInstance()} callers who run outside Spring).
 *
 * @param <T> domain aggregate type (Contact, Task, Appointment, etc.)
 */
public interface DomainDataStore<T> {

    /**
     * @param id identifier to look up (already validated by callers)
     * @return {@code true} if an aggregate with the id exists
     */
    boolean existsById(String id);

    /**
     * Persists (or updates) the aggregate.
     *
     * @param aggregate fully validated aggregate copy
     */
    void save(T aggregate);

    /**
     * @param id identifier to look up
     * @return aggregate if found
     */
    Optional<T> findById(String id);

    /**
     * @return all aggregates currently stored
     */
    List<T> findAll();

    /**
     * Deletes by id if present.
     *
     * @param id identifier to delete
     * @return {@code true} if something was removed
     */
    boolean deleteById(String id);

    /**
     * Utility hook for test isolation.
     */
    void deleteAll();
}
