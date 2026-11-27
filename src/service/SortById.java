package service;

import model.Task;
import java.util.Comparator;

public class SortById implements SortStrategy {
    @Override
    public Comparator<Task> comparator(boolean ascending) {
        Comparator<Task> cmp = Comparator.comparingInt(Task::getId);
        return ascending ? cmp : cmp.reversed();
    }
}