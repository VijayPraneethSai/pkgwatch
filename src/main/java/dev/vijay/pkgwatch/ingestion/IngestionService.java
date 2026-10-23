package dev.vijay.pkgwatch.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vijay.pkgwatch.detection.Anomaly;
import dev.vijay.pkgwatch.detection.DetectionService;
import dev.vijay.pkgwatch.export.AlertExporter;
import dev.vijay.pkgwatch.llm.IncidentSummarizer;
import dev.vijay.pkgwatch.llm.IncidentSummary;
import dev.vijay.pkgwatch.model.UsageReading;
import dev.vijay.pkgwatch.persistence.Incident;
import dev.vijay.pkgwatch.persistence.IncidentRepository;
import dev.vijay.pkgwatch.persistence.ProcessedEvent;
import dev.vijay.pkgwatch.persistence.ProcessedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class IngestionService {

    private final ObjectMapper mapper;
    private final DetectionService detection;
    private final ProcessedEventRepository processedEvents;
    private final IncidentRepository incidents;
    private final IncidentSummarizer summarizer;
    private final AlertExporter exporter;
    private final MeterRegistry registry;
    private final Counter processed;
    private final Counter duplicates;
    private final AtomicReference<Instant> lastProcessed = new AtomicReference<>();

    public IngestionService(ObjectMapper mapper, DetectionService detection,
                            ProcessedEventRepository processedEvents, IncidentRepository incidents,
                            IncidentSummarizer summarizer, AlertExporter exporter, MeterRegistry registry) {
        this.mapper = mapper;
        this.detection = detection;
        this.processedEvents = processedEvents;
        this.incidents = incidents;
        this.summarizer = summarizer;
        this.exporter = exporter;
        this.registry = registry;
        this.processed = Counter.builder("pkgwatch.readings.processed").register(registry);
        this.duplicates = Counter.builder("pkgwatch.readings.duplicates").register(registry);
    }

    public void process(String payload) {
        UsageReading reading = parse(payload);
        reading.validate();

        if (processedEvents.existsById(reading.eventId())) {
            duplicates.increment();
            return;
        }

        List<Anomaly> anomalies = detection.evaluate(reading);
        for (Anomaly anomaly : anomalies) {
            Incident incident = Incident.open(reading, anomaly);
            IncidentSummary summary = summarizer.summarize(reading, anomaly);
            incident.describe(summary.summary(), summary.likelyCause());
            incidents.save(incident);
            registry.counter("pkgwatch.incidents.created", "type", anomaly.type().name()).increment();
            exporter.export(incident);
        }

        processedEvents.save(new ProcessedEvent(reading.eventId(), Instant.now()));
        processed.increment();
        lastProcessed.set(Instant.now());
    }

    public Instant lastProcessed() {
        return lastProcessed.get();
    }

    private UsageReading parse(String payload) {
        try {
            return mapper.readValue(payload, UsageReading.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("malformed payload: " + e.getOriginalMessage(), e);
        }
    }
}
