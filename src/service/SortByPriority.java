package service;

import model.Task;
import java.util.Comparator;

public class SortByPriority implements SortStrategy {
    @Override
    public Comparator<Task> comparator(boolean ascending) {
        Comparator<Task> cmp = Comparator.comparing(
                t -> t.getPriority() == null ? 99 : t.getPriority().ordinal()
        );
        return ascending ? cmp : cmp.reversed();
    }
}