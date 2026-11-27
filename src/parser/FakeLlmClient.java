package parser;

public class FakeLlmClient extends LlmClient {
    private final String fixedResponse;

    public FakeLlmClient(String fixedResponse) {
        super("fake-test-key");
        this.fixedResponse = fixedResponse;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return fixedResponse;
    }
}