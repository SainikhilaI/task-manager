package service;

import model.Task;

public class StatusFilter implements TaskFilter {
    private final boolean completed;

    public StatusFilter(boolean completed) {
        this.completed = completed;
    }

    @Override
    public boolean matches(Task task) {
        return task != null && task.isCompleted() == completed;
    }
}