package dev.vijay.pkgwatch.llm;

import dev.vijay.pkgwatch.config.PkgwatchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class LlmConfig {

    @Bean
    LlmClient llmClient(RestClient.Builder builder, PkgwatchProperties props) {
        return new GeminiClient(builder, props.llm());
    }
}
