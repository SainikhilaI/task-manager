package model;

import java.time.LocalDate;

public class TaskQuery {
    private final Boolean completed;
    private final Priority priority;
    private final LocalDate dueBefore;
    private final LocalDate dueAfter;
    private final String searchText;
    private final String sortBy;
    private final boolean ascending;
    private final int page;
    private final int pageSize;

    private TaskQuery(Builder builder) {
        this.completed = builder.completed;
        this.priority = builder.priority;
        this.dueBefore = builder.dueBefore;
        this.dueAfter = builder.dueAfter;
        this.searchText = builder.searchText;
        this.sortBy = builder.sortBy;
        this.ascending = builder.ascending;
        this.page = builder.page;
        this.pageSize = builder.pageSize;
    }

    public Boolean getCompleted() { return completed; }
    public Priority getPriority() { return priority; }
    public LocalDate getDueBefore() { return dueBefore; }
    public LocalDate getDueAfter() { return dueAfter; }
    public String getSearchText() { return searchText; }
    public String getSortBy() { return sortBy; }
    public boolean isAscending() { return ascending; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }

    public static class Builder {
        private Boolean completed;
        private Priority priority;
        private LocalDate dueBefore;
        private LocalDate dueAfter;
        private String searchText;
        private String sortBy = "id";
        private boolean ascending = true;
        private int page = 1;
        private int pageSize = 10;

        public Builder completed(Boolean completed) { this.completed = completed; return this; }
        public Builder priority(Priority priority) { this.priority = priority; return this; }
        public Builder dueBefore(LocalDate dueBefore) { this.dueBefore = dueBefore; return this; }
        public Builder dueAfter(LocalDate dueAfter) { this.dueAfter = dueAfter; return this; }
        public Builder searchText(String searchText) { this.searchText = searchText; return this; }
        public Builder sortBy(String sortBy) { this.sortBy = sortBy; return this; }
        public Builder ascending(boolean ascending) { this.ascending = ascending; return this; }
        public Builder page(int page) { this.page = page; return this; }
        public Builder pageSize(int pageSize) { this.pageSize = pageSize; return this; }

        public TaskQuery build() {
            return new TaskQuery(this);
        }
    }
}