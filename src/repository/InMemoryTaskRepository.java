package repository;

import model.Task;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryTaskRepository implements TaskRepository {
    private static volatile InMemoryTaskRepository instance;

    private final Map<Integer, Task> store = new LinkedHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    private InMemoryTaskRepository() {
    }

    public static InMemoryTaskRepository getInstance() {
        if (instance == null) {
            synchronized (InMemoryTaskRepository.class) {
                if (instance == null) {
                    instance = new InMemoryTaskRepository();
                }
            }
        }
        return instance;
    }

    @Override
    public synchronized Task save(Task task) {
        int assignedId = task.getId() > 0 ? task.getId() : idSequence.incrementAndGet();
        if (assignedId > idSequence.get()) {
            idSequence.set(assignedId);
        }
        Task saved = task.toBuilder().id(assignedId).build();
        store.put(assignedId, saved);
        return saved;
    }

    @Override
    public synchronized Optional<Task> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public synchronized List<Task> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public synchronized boolean delete(int id) {
        return store.remove(id) != null;
    }

    @Override
    public synchronized void clear() {
        store.clear();
        idSequence.set(0);
    }
}