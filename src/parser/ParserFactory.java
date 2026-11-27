package parser;

import exception.ServiceUnavailableException;

public class ParserFactory {
    public enum Mode {
        AUTO_LLM,
        RULE_BASED
    }

    private ParserFactory() {
    }

    public static TaskParser createParser(Mode mode, LlmClient customClient) {
        if (mode == Mode.RULE_BASED) {
            return new RuleBasedTaskParser();
        }
        if (customClient != null) {
            return new LlmTaskParser(customClient);
        }
        String apiKey = System.getenv("LLM_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new ServiceUnavailableException("LLM_API_KEY environment variable is missing");
        }
        return new LlmTaskParser(new LlmClient(apiKey));
    }

    public static TaskParser createDefaultParser() {
        return createParser(Mode.AUTO_LLM, null);
    }
}