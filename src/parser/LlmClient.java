package parser;

import exception.BadGatewayException;
import exception.ServiceUnavailableException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LlmClient {
    private static final String GEMINI_ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=%s";

    private final String apiKey;
    private final HttpClient httpClient;

    public LlmClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ServiceUnavailableException("LLM_API_KEY is not configured");
        }

        String combinedPrompt = systemPrompt + "\n\n" + userPrompt;
        String payload = "{\"contents\":[{\"parts\":[{\"text\":"
                + SimpleJson.quote(combinedPrompt) + "}]}]}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(GEMINI_ENDPOINT_TEMPLATE, apiKey)))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadGatewayException("LLM provider returned HTTP " + response.statusCode());
            }

            String extracted = SimpleJson.extractFirstStringValue(response.body(), "text");
            if (extracted == null) {
                throw new BadGatewayException("LLM response did not contain readable text");
            }
            return extracted.trim();
        } catch (ServiceUnavailableException | BadGatewayException e) {
            throw e;
        } catch (Exception e) {
            throw new BadGatewayException("LLM request failed or timed out: " + e.getMessage());
        }
    }
}