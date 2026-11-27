package parser;

import exception.BadGatewayException;
import model.TaskRequest;

import java.util.ArrayList;
import java.util.List;

public class LlmBreakdownStrategy implements BreakdownStrategy {
    private final LlmClient client;

    public LlmBreakdownStrategy(LlmClient client) {
        this.client = client;
    }

    @Override
    public List<TaskRequest> breakDown(String taskTitle, String taskDescription) {
        String systemPrompt = "Break a task into concrete actionable subtasks. "
                + "Output ONE subtask per line in exact format: title|description. "
                + "Do not include numbering or extra text.";
        String userPrompt = "Task Title: " + taskTitle + "\nTask Description: " + taskDescription;

        String raw = client.complete(systemPrompt, userPrompt);
        if (raw == null || raw.isBlank()) {
            throw new BadGatewayException("LLM returned empty breakdown response");
        }

        String cleaned = SimpleJson.stripCodeFences(raw).trim();
        String[] lines = cleaned.split("\\R");
        List<TaskRequest> results = new ArrayList<>();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;
            String[] parts = trimmedLine.split("\\|", 2);
            if (parts.length < 2) {
                throw new BadGatewayException("Malformed subtask line from LLM: " + trimmedLine);
            }
            results.add(new TaskRequest(parts[0].trim(), parts[1].trim(), "false", null, "MEDIUM"));
        }

        if (results.isEmpty()) {
            throw new BadGatewayException("LLM breakdown produced zero subtasks");
        }
        return results;
    }
}