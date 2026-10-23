package dev.vijay.pkgwatch.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vijay.pkgwatch.detection.Anomaly;
import dev.vijay.pkgwatch.detection.AnomalyType;
import dev.vijay.pkgwatch.detection.Severity;
import dev.vijay.pkgwatch.model.UsageReading;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncidentSummarizerTest {

    private final UsageReading reading = new UsageReading("lodash", 10, Instant.parse("2026-01-10T00:00:00Z"), 5000);
    private final Anomaly anomaly = new Anomaly(AnomalyType.SPIKE, Severity.HIGH, "5000 downloads is far above the mean");
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void parsesStructuredModelResponse() {
        LlmClient llm = mock(LlmClient.class);
        when(llm.isAvailable()).thenReturn(true);
        when(llm.complete(anyString(), anyString()))
                .thenReturn("{\"summary\":\"Downloads jumped sharply.\",\"likelyCause\":\"Possibly a CI mirror.\"}");

        IncidentSummary result = new IncidentSummarizer(llm, new ObjectMapper(), registry).summarize(reading, anomaly);

        assertEquals("Downloads jumped sharply.", result.summary());
        assertEquals("Possibly a CI mirror.", result.likelyCause());
    }

    @Test
    void fallsBackToTemplateWhenModelFails() {
        LlmClient llm = mock(LlmClient.class);
        when(llm.isAvailable()).thenReturn(true);
        when(llm.complete(anyString(), anyString())).thenThrow(new LlmUnavailableException("timeout"));

        IncidentSummary result = new IncidentSummarizer(llm, new ObjectMapper(), registry).summarize(reading, anomaly);

        assertTrue(result.summary().contains("lodash"));
        assertEquals(1.0, registry.get("pkgwatch.llm.fallbacks").counter().count());
    }

    @Test
    void fallsBackWhenResponseIsNotJson() {
        LlmClient llm = mock(LlmClient.class);
        when(llm.isAvailable()).thenReturn(true);
        when(llm.complete(anyString(), anyString())).thenReturn("not json");

        IncidentSummary result = new IncidentSummarizer(llm, new ObjectMapper(), registry).summarize(reading, anomaly);

        assertTrue(result.summary().contains("SPIKE"));
    }

    @Test
    void skipsTheModelWhenUnavailable() {
        LlmClient llm = mock(LlmClient.class);
        when(llm.isAvailable()).thenReturn(false);

        new IncidentSummarizer(llm, new ObjectMapper(), registry).summarize(reading, anomaly);

        verify(llm, never()).complete(anyString(), anyString());
    }
}
