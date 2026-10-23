package dev.vijay.pkgwatch.llm;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vijay.pkgwatch.config.PkgwatchProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class GeminiClient implements LlmClient {

    private final RestClient http;
    private final String model;
    private final String apiKey;

    public GeminiClient(RestClient.Builder builder, PkgwatchProperties.Llm config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(config.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(config.timeoutSeconds()));
        this.http = builder.baseUrl(config.baseUrl()).requestFactory(factory).build();
        this.model = config.model();
        this.apiKey = config.apiKey() == null ? "" : config.apiKey();
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isBlank();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new LlmUnavailableException("no api key configured");
        }
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("temperature", 0.2, "responseMimeType", "application/json"));
        try {
            JsonNode response = http.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode text = response == null ? null : response.at("/candidates/0/content/parts/0/text");
            if (text == null || text.isMissingNode() || text.asText().isBlank()) {
                throw new LlmUnavailableException("empty response from model");
            }
            return text.asText();
        } catch (RestClientException e) {
            throw new LlmUnavailableException("model request failed: " + e.getClass().getSimpleName());
        }
    }
}
