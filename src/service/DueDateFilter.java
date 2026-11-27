package service;

import model.Task;
import java.time.LocalDate;

public class DueDateFilter implements TaskFilter {
    private final LocalDate dueBefore;
    private final LocalDate dueAfter;

    public DueDateFilter(LocalDate dueBefore, LocalDate dueAfter) {
        this.dueBefore = dueBefore;
        this.dueAfter = dueAfter;
    }

    @Override
    public boolean matches(Task task) {
        if (task == null) return false;
        LocalDate due = task.getDueDate();
        if (due == null) return false;
        if (dueBefore != null && due.isAfter(dueBefore)) return false;
        if (dueAfter != null && due.isBefore(dueAfter)) return false;
        return true;
    }
}