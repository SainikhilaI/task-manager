package service;

import event.TaskEventListener;
import exception.BadGatewayException;
import exception.FieldErrorDetail;
import exception.NotFoundException;
import exception.ValidationException;
import model.Priority;
import model.Task;
import model.TaskQuery;
import model.TaskRequest;
import parser.BreakdownStrategy;
import parser.ParsedTask;
import parser.RuleBasedBreakdownStrategy;
import parser.RuleBasedTaskParser;
import parser.TaskParser;
import repository.TaskRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskService {
    private final TaskRepository repository;
    private final List<TaskEventListener> listeners = new ArrayList<>();
    private final Map<String, SortStrategy> sortStrategies = new HashMap<>();
    private TaskParser taskParser;
    private BreakdownStrategy breakdownStrategy;

    public TaskService(TaskRepository repository) {
        this(repository, new RuleBasedTaskParser(), new RuleBasedBreakdownStrategy());
    }

    public TaskService(TaskRepository repository, TaskParser taskParser, BreakdownStrategy breakdownStrategy) {
        this.repository = repository;
        this.taskParser = taskParser;
        this.breakdownStrategy = breakdownStrategy;
        registerDefaultSortStrategies();
    }

    private void registerDefaultSortStrategies() {
        sortStrategies.put("id", new SortById());
        sortStrategies.put("priority", new SortByPriority());
        sortStrategies.put("dueDate", new SortByDueDate());
    }

    public void registerSortStrategy(String name, SortStrategy strategy) {
        if (name != null && strategy != null) {
            sortStrategies.put(name, strategy);
        }
    }

    public void addListener(TaskEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(TaskEventListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String eventType, Task task) {
        for (TaskEventListener listener : new ArrayList<>(listeners)) {
            listener.onEvent(eventType, task);
        }
    }

    public void setTaskParser(TaskParser taskParser) {
        this.taskParser = taskParser;
    }

    public void setBreakdownStrategy(BreakdownStrategy breakdownStrategy) {
        this.breakdownStrategy = breakdownStrategy;
    }

    public Task createTask(TaskRequest request) {
        ValidatedFields valid = validateRequest(request);
        Task task = new Task.Builder(valid.title, valid.description, valid.completed)
                .dueDate(valid.dueDate)
                .priority(valid.priority)
                .build();
        Task saved = repository.save(task);
        notifyListeners("TASK_CREATED", saved);
        return saved;
    }

    public Task createTaskFromText(String text) {
        if (text == null || text.isBlank()) {
            throw new BadGatewayException("Input text for task parsing must not be empty",
                    List.of(new FieldErrorDetail("text", "must not be empty")));
        }
        String today = LocalDate.now().toString();
        ParsedTask parsed = taskParser.parse(text, today);
        if (parsed == null) {
            throw new BadGatewayException("Parser returned null task");
        }

        TaskRequest candidate = new TaskRequest(
                parsed.getTitle(),
                parsed.getDescription(),
                "false",
                parsed.getDueDate(),
                "MEDIUM"
        );

        ValidatedFields valid;
        try {
            valid = validateRequest(candidate);
        } catch (ValidationException ve) {
            throw new BadGatewayException("Parsed task failed validation: " + ve.getMessage(), ve.getDetails());
        }

        Task task = new Task.Builder(valid.title, valid.description, valid.completed)
                .dueDate(valid.dueDate)
                .priority(valid.priority)
                .recurrence(parsed.getRecurrence())
                .build();
        Task saved = repository.save(task);
        notifyListeners("TASK_CREATED", saved);
        return saved;
    }

    public Task createSubtask(int parentId, TaskRequest request) {
        Task parent = getTaskById(parentId);
        if (parent.getParentId() != null) {
            throw new ValidationException("parentId", "cannot create a subtask of a subtask");
        }
        ValidatedFields valid = validateRequest(request);
        Task subtask = new Task.Builder(valid.title, valid.description, valid.completed)
                .dueDate(valid.dueDate)
                .priority(valid.priority)
                .parentId(parentId)
                .build();
        Task saved = repository.save(subtask);
        notifyListeners("TASK_CREATED", saved);
        return saved;
    }

    public List<Task> listSubtasks(int parentId) {
        getTaskById(parentId);
        List<Task> result = new ArrayList<>();
        for (Task t : repository.findAll()) {
            if (t.getParentId() != null && t.getParentId() == parentId) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Task> breakDownTask(int id) {
        Task target = getTaskById(id);
        if (target.getParentId() != null) {
            throw new ValidationException("id", "cannot break down a task that is already a subtask");
        }

        List<TaskRequest> proposals = breakdownStrategy.breakDown(target.getTitle(), target.getDescription());
        if (proposals == null || proposals.isEmpty()) {
            throw new BadGatewayException("Breakdown strategy returned no subtasks");
        }

        List<ValidatedFields> validatedSubtasks = new ArrayList<>();
        for (TaskRequest req : proposals) {
            try {
                validatedSubtasks.add(validateRequest(req));
            } catch (ValidationException ve) {
                throw new BadGatewayException("Generated subtask failed validation: " + ve.getMessage(), ve.getDetails());
            }
        }

        List<Task> savedSubtasks = new ArrayList<>();
        for (ValidatedFields valid : validatedSubtasks) {
            Task sub = new Task.Builder(valid.title, valid.description, valid.completed)
                    .dueDate(valid.dueDate)
                    .priority(valid.priority)
                    .parentId(target.getId())
                    .build();
            Task saved = repository.save(sub);
            savedSubtasks.add(saved);
            notifyListeners("TASK_CREATED", saved);
        }
        return savedSubtasks;
    }

    public Task getTaskById(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found with id: " + id));
    }

    public List<Task> getAllTasks() {
        return repository.findAll();
    }

    public List<Task> queryTasks(TaskQuery query) {
        TaskQuery effective = (query == null) ? new TaskQuery.Builder().build() : query;
        if (effective.getPage() < 1) {
            throw new ValidationException("page", "must be greater than or equal to 1");
        }
        if (effective.getPageSize() < 1) {
            throw new ValidationException("pageSize", "must be greater than or equal to 1");
        }

        List<TaskFilter> filters = new ArrayList<>();
        if (effective.getCompleted() != null) {
            filters.add(new StatusFilter(effective.getCompleted()));
        }
        if (effective.getPriority() != null) {
            filters.add(new PriorityFilter(effective.getPriority()));
        }
        if (effective.getDueBefore() != null || effective.getDueAfter() != null) {
            filters.add(new DueDateFilter(effective.getDueBefore(), effective.getDueAfter()));
        }

        String search = effective.getSearchText() == null ? "" : effective.getSearchText().trim().toLowerCase();

        List<Task> matched = new ArrayList<>();
        for (Task task : repository.findAll()) {
            boolean passFilters = true;
            for (TaskFilter filter : filters) {
                if (!filter.matches(task)) {
                    passFilters = false;
                    break;
                }
            }
            if (!passFilters) continue;

            if (!search.isEmpty()) {
                String title = task.getTitle() == null ? "" : task.getTitle().toLowerCase();
                String desc = task.getDescription() == null ? "" : task.getDescription().toLowerCase();
                if (!title.contains(search) && !desc.contains(search)) {
                    continue;
                }
            }
            matched.add(task);
        }

        String sortKey = effective.getSortBy() == null ? "id" : effective.getSortBy();
        SortStrategy strategy = sortStrategies.getOrDefault(sortKey, new SortById());
        Comparator<Task> comparator = strategy.comparator(effective.isAscending());
        matched.sort(comparator);

        int fromIndex = (effective.getPage() - 1) * effective.getPageSize();
        if (fromIndex >= matched.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + effective.getPageSize(), matched.size());
        return matched.subList(fromIndex, toIndex);
    }

    public Task updateTask(int id, TaskRequest request) {
        Task existing = getTaskById(id);
        ValidatedFields valid = validateRequest(request);
        Task rebuilt = new Task.Builder(valid.title, valid.description, valid.completed)
                .id(existing.getId())
                .dueDate(valid.dueDate)
                .priority(valid.priority)
                .recurrence(existing.getRecurrence())
                .parentId(existing.getParentId())
                .build();
        Task saved = repository.save(rebuilt);
        notifyListeners("TASK_UPDATED", saved);
        return saved;
    }

    public Task completeTask(int id) {
        Task existing = getTaskById(id);
        Task completed = existing.toBuilder().completed(true).build();
        Task saved = repository.save(completed);
        notifyListeners("TASK_COMPLETED", saved);
        return saved;
    }

    public void deleteTask(int id) {
        Task existing = getTaskById(id);
        List<Task> all = repository.findAll();
        for (Task t : all) {
            if (t.getParentId() != null && t.getParentId() == id) {
                repository.delete(t.getId());
                notifyListeners("TASK_DELETED", t);
            }
        }
        repository.delete(id);
        notifyListeners("TASK_DELETED", existing);
    }

    public ValidatedFields validateRequest(TaskRequest request) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new FieldErrorDetail("request", "must not be null"));
            throw new ValidationException("Validation failed", errors);
        }

        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            errors.add(new FieldErrorDetail("title", "must not be empty"));
        }

        String description = request.getDescription();
        if (description == null || description.trim().isEmpty()) {
            errors.add(new FieldErrorDetail("description", "must not be empty"));
        }

        String completedStr = request.getCompleted();
        boolean completed = false;
        if (!"true".equals(completedStr) && !"false".equals(completedStr)) {
            errors.add(new FieldErrorDetail("completed", "must be \"true\" or \"false\""));
        } else {
            completed = Boolean.parseBoolean(completedStr);
        }

        LocalDate dueDate = null;
        String dueDateStr = request.getDueDate();
        if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
            dueDate = parseStrictIsoDate(dueDateStr.trim());
            if (dueDate == null) {
                errors.add(new FieldErrorDetail("dueDate", "must be a valid calendar date in YYYY-MM-DD format"));
            }
        }

        Priority priority = Priority.MEDIUM;
        String priorityStr = request.getPriority();
        if (priorityStr != null && !priorityStr.trim().isEmpty()) {
            try {
                priority = Priority.valueOf(priorityStr.trim());
            } catch (IllegalArgumentException ex) {
                errors.add(new FieldErrorDetail("priority", "must be LOW, MEDIUM, or HIGH"));
            }
        } else {
            errors.add(new FieldErrorDetail("priority", "must be LOW, MEDIUM, or HIGH"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }

        return new ValidatedFields(title.trim(), description.trim(), completed, dueDate, priority);
    }

    public static LocalDate parseStrictIsoDate(String value) {
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return null;
        }
        int year = Integer.parseInt(value.substring(0, 4));
        int month = Integer.parseInt(value.substring(5, 7));
        int day = Integer.parseInt(value.substring(8, 10));

        if (month < 1 || month > 12) return null;
        int maxDays = daysInMonth(year, month);
        if (day < 1 || day > maxDays) return null;

        return LocalDate.of(year, month, day);
    }

    private static int daysInMonth(int year, int month) {
        return switch (month) {
            case 1, 3, 5, 7, 8, 10, 12 -> 31;
            case 4, 6, 9, 11 -> 30;
            case 2 -> isLeapYear(year) ? 29 : 28;
            default -> 0;
        };
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    public static class ValidatedFields {
        public final String title;
        public final String description;
        public final boolean completed;
        public final LocalDate dueDate;
        public final Priority priority;

        public ValidatedFields(String title, String description, boolean completed, LocalDate dueDate, Priority priority) {
            this.title = title;
            this.description = description;
            this.completed = completed;
            this.dueDate = dueDate;
            this.priority = priority;
        }
    }
}