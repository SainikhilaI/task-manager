package parser;

import model.TaskRequest;

import java.util.ArrayList;
import java.util.List;

public class RuleBasedBreakdownStrategy implements BreakdownStrategy {
    @Override
    public List<TaskRequest> breakDown(String taskTitle, String taskDescription) {
        String source = (taskDescription != null && !taskDescription.isBlank()) ? taskDescription : taskTitle;
        String normalized = source == null ? "" : source.replace(" and ", ",");
        String[] rawParts = normalized.split(",");

        List<TaskRequest> subtasks = new ArrayList<>();
        for (String part : rawParts) {
            String item = part.trim();
            if (!item.isEmpty()) {
                subtasks.add(new TaskRequest(item, "Subtask for: " + taskTitle, "false", null, "MEDIUM"));
            }
        }
        if (subtasks.isEmpty() && taskTitle != null && !taskTitle.isBlank()) {
            subtasks.add(new TaskRequest("Complete " + taskTitle.trim(), taskTitle.trim(), "false", null, "MEDIUM"));
        }
        return subtasks;
    }
}