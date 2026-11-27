package parser;

import exception.BadGatewayException;

public class LlmTaskParser implements TaskParser {
    private final LlmClient client;

    public LlmTaskParser(LlmClient client) {
        this.client = client;
    }

    @Override
    public ParsedTask parse(String text, String today) {
        String systemPrompt = "You extract structured task fields from plain English. "
                + "Reply ONLY with a valid JSON object containing keys: "
                + "title, description, dueDate (YYYY-MM-DD or null), recurrence (string or null). "
                + "Do not wrap in markdown or add extra commentary.";
        String userPrompt = "Today is " + today + ". Task text: " + text;

        String rawReply = client.complete(systemPrompt, userPrompt);
        if (rawReply == null || rawReply.isBlank()) {
            throw new BadGatewayException("LLM returned empty task payload");
        }

        String title = SimpleJson.extractString(rawReply, "title");
        String description = SimpleJson.extractString(rawReply, "description");
        String dueDate = SimpleJson.extractString(rawReply, "dueDate");
        String recurrence = SimpleJson.extractString(rawReply, "recurrence");

        return new ParsedTask(title, description, dueDate, recurrence);
    }
}