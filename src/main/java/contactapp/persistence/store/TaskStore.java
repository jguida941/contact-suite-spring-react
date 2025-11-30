package contactapp.persistence.store;

import contactapp.domain.Task;

/**
 * Task-specific persistence abstraction layered over {@link DomainDataStore}.
 */
public interface TaskStore extends DomainDataStore<Task> {
}
