package dev.vijay.pkgwatch.observability;

import dev.vijay.pkgwatch.ingestion.IngestionService;
import dev.vijay.pkgwatch.persistence.IncidentRepository;
import dev.vijay.pkgwatch.persistence.IncidentStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class PipelineHealthIndicator implements HealthIndicator {

    private static final Duration STALE_AFTER = Duration.ofMinutes(2);

    private final IngestionService ingestion;
    private final IncidentRepository incidents;
    private final MeterRegistry registry;

    public PipelineHealthIndicator(IngestionService ingestion, IncidentRepository incidents, MeterRegistry registry) {
        this.ingestion = ingestion;
        this.incidents = incidents;
        this.registry = registry;
    }

    @Override
    public Health health() {
        Instant last = ingestion.lastProcessed();
        Counter dlq = registry.find("pkgwatch.dlq.published").counter();

        Health.Builder builder;
        if (last == null) {
            builder = Health.unknown().withDetail("lastProcessed", "nothing processed yet");
        } else if (Duration.between(last, Instant.now()).compareTo(STALE_AFTER) > 0) {
            builder = Health.status("STALE").withDetail("lastProcessed", last.toString());
        } else {
            builder = Health.up().withDetail("lastProcessed", last.toString());
        }
        return builder
                .withDetail("deadLettersPublished", dlq == null ? 0 : (long) dlq.count())
                .withDetail("openIncidents", incidents.countByStatus(IncidentStatus.OPEN))
                .build();
    }
}
