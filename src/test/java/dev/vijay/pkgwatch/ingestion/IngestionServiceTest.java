package dev.vijay.pkgwatch.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vijay.pkgwatch.detection.Anomaly;
import dev.vijay.pkgwatch.detection.AnomalyType;
import dev.vijay.pkgwatch.detection.DetectionService;
import dev.vijay.pkgwatch.detection.Severity;
import dev.vijay.pkgwatch.export.AlertExporter;
import dev.vijay.pkgwatch.llm.IncidentSummarizer;
import dev.vijay.pkgwatch.llm.IncidentSummary;
import dev.vijay.pkgwatch.model.UsageReading;
import dev.vijay.pkgwatch.persistence.Incident;
import dev.vijay.pkgwatch.persistence.IncidentRepository;
import dev.vijay.pkgwatch.persistence.ProcessedEvent;
import dev.vijay.pkgwatch.persistence.ProcessedEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngestionServiceTest {

    private static final String VALID =
            "{\"packageName\":\"lodash\",\"sequence\":7,\"timestamp\":\"2026-01-08T00:00:00Z\",\"downloads\":1200}";

    private final ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    private final DetectionService detection = mock(DetectionService.class);
    private final ProcessedEventRepository processedEvents = mock(ProcessedEventRepository.class);
    private final IncidentRepository incidents = mock(IncidentRepository.class);
    private final IncidentSummarizer summarizer = mock(IncidentSummarizer.class);
    private final AlertExporter exporter = mock(AlertExporter.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private IngestionService service;

    @BeforeEach
    void setUp() {
        service = new IngestionService(mapper, detection, processedEvents, incidents, summarizer, exporter, registry);
    }

    @Test
    void malformedJsonIsRejectedAsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.process("{not json"));
    }

    @Test
    void invalidReadingIsRejected() {
        String noName = "{\"packageName\":\"\",\"sequence\":1,\"timestamp\":\"2026-01-08T00:00:00Z\",\"downloads\":5}";
        assertThrows(IllegalArgumentException.class, () -> service.process(noName));
    }

    @Test
    void duplicateEventsAreSkipped() {
        when(processedEvents.existsById("lodash:7")).thenReturn(true);

        service.process(VALID);

        verify(detection, never()).evaluate(any());
        assertEquals(1.0, registry.get("pkgwatch.readings.duplicates").counter().count());
    }

    @Test
    void anomalyCreatesIncidentAndExportsAlert() {
        Anomaly anomaly = new Anomaly(AnomalyType.SPIKE, Severity.HIGH, "far above the mean");
        when(detection.evaluate(any(UsageReading.class))).thenReturn(List.of(anomaly));
        when(summarizer.summarize(any(), any())).thenReturn(new IncidentSummary("Downloads spiked.", "Unknown"));

        service.process(VALID);

        verify(incidents).save(any(Incident.class));
        verify(exporter).export(any(Incident.class));
        verify(processedEvents).save(any(ProcessedEvent.class));
        assertNotNull(service.lastProcessed());
        assertEquals(1.0, registry.get("pkgwatch.readings.processed").counter().count());
    }

    @Test
    void quietReadingOnlyRecordsTheEvent() {
        when(detection.evaluate(any(UsageReading.class))).thenReturn(List.of());

        service.process(VALID);

        verify(incidents, never()).save(any());
        verify(exporter, never()).export(any());
        verify(processedEvents).save(any(ProcessedEvent.class));
    }
}
