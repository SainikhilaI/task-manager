package model;

import java.time.LocalDate;

public class Task {
    private final int id;
    private final String title;
    private final String description;
    private final boolean completed;
    private final LocalDate dueDate;
    private final Priority priority;
    private final String recurrence;
    private final Integer parentId;

    private Task(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.completed = builder.completed;
        this.dueDate = builder.dueDate;
        this.priority = builder.priority;
        this.recurrence = builder.recurrence;
        this.parentId = builder.parentId;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
    public LocalDate getDueDate() { return dueDate; }
    public Priority getPriority() { return priority; }
    public String getRecurrence() { return recurrence; }
    public Integer getParentId() { return parentId; }

    public Builder toBuilder() {
        return new Builder(this.title, this.description, this.completed)
                .id(this.id)
                .dueDate(this.dueDate)
                .priority(this.priority)
                .recurrence(this.recurrence)
                .parentId(this.parentId);
    }

    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"title\":\"" + escape(title) + "\","
                + "\"description\":\"" + escape(description) + "\","
                + "\"completed\":" + completed + ","
                + "\"dueDate\":" + (dueDate == null ? "null" : "\"" + dueDate + "\"") + ","
                + "\"priority\":" + (priority == null ? "null" : "\"" + priority.name() + "\"") + ","
                + "\"recurrence\":" + (recurrence == null ? "null" : "\"" + escape(recurrence) + "\"") + ","
                + "\"parentId\":" + (parentId == null ? "null" : parentId)
                + "}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static class Builder {
        private final String title;
        private final String description;
        private boolean completed;
        private int id;
        private LocalDate dueDate;
        private Priority priority = Priority.MEDIUM;
        private String recurrence;
        private Integer parentId;

        public Builder(String title, String description, boolean completed) {
            this.title = title;
            this.description = description;
            this.completed = completed;
        }

        public Builder completed(boolean completed) { this.completed = completed; return this; }
        public Builder id(int id) { this.id = id; return this; }
        public Builder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public Builder priority(Priority priority) { this.priority = priority; return this; }
        public Builder recurrence(String recurrence) { this.recurrence = recurrence; return this; }
        public Builder parentId(Integer parentId) { this.parentId = parentId; return this; }

        public Task build() {
            return new Task(this);
        }
    }
}