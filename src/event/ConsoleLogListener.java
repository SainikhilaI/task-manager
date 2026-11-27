package event;

import model.Task;

public class ConsoleLogListener implements TaskEventListener {
    @Override
    public void onEvent(String eventType, Task task) {
        String taskSummary = task == null ? "null" : ("id=" + task.getId() + ", title=\"" + task.getTitle() + "\"");
        System.out.println("[EVENT] " + eventType + " -> " + taskSummary);
    }
}