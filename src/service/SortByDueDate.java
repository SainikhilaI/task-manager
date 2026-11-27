package service;

import model.Task;
import java.time.LocalDate;
import java.util.Comparator;

public class SortByDueDate implements SortStrategy {
    @Override
    public Comparator<Task> comparator(boolean ascending) {
        LocalDate sentinel = ascending ? LocalDate.MAX : LocalDate.MIN;
        Comparator<Task> cmp = Comparator.comparing(
                t -> t.getDueDate() == null ? sentinel : t.getDueDate()
        );
        return ascending ? cmp : cmp.reversed();
    }
}