package dev.vijay.pkgwatch.llm;

public interface LlmClient {

    boolean isAvailable();

    String complete(String systemPrompt, String userPrompt);
}
