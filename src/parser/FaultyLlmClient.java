package parser;

import exception.BadGatewayException;
import exception.ServiceUnavailableException;

public class FaultyLlmClient extends LlmClient {
    public enum FailureMode {
        BAD_GATEWAY,
        MALFORMED_JSON,
        UNAVAILABLE
    }

    private final FailureMode failureMode;

    public FaultyLlmClient(FailureMode failureMode) {
        super("faulty-test-key");
        this.failureMode = failureMode;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return switch (failureMode) {
            case BAD_GATEWAY -> throw new BadGatewayException("Simulated LLM upstream 502 error");
            case MALFORMED_JSON -> "not valid json {{{";
            case UNAVAILABLE -> throw new ServiceUnavailableException("Simulated LLM service unavailable");
        };
    }
}