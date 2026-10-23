package dev.vijay.pkgwatch.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vijay.pkgwatch.config.PkgwatchProperties;
import dev.vijay.pkgwatch.persistence.Incident;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Instant;

@Component
public class WebhookAlertExporter implements AlertExporter {

    private static final Logger log = LoggerFactory.getLogger(WebhookAlertExporter.class);
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient http;
    private final ObjectMapper mapper;
    private final PkgwatchProperties.Webhook config;
    private final Counter delivered;
    private final Counter failed;

    public WebhookAlertExporter(RestClient.Builder builder, PkgwatchProperties props,
                                ObjectMapper mapper, MeterRegistry registry) {
        this.http = builder.build();
        this.mapper = mapper;
        this.config = props.webhook();
        this.delivered = Counter.builder("pkgwatch.webhook.delivered").register(registry);
        this.failed = Counter.builder("pkgwatch.webhook.failed").register(registry);
    }

    @Override
    public void export(Incident incident) {
        if (config.url() == null || config.url().isBlank()) {
            return;
        }
        String body;
        try {
            body = mapper.writeValueAsString(AlertPayload.from(incident));
        } catch (JsonProcessingException e) {
            failed.increment();
            log.warn("could not serialize alert for incident {}", incident.getId());
            return;
        }
        long timestamp = Instant.now().getEpochSecond();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                RestClient.RequestBodySpec request = http.post()
                        .uri(URI.create(config.url()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Pkgwatch-Timestamp", Long.toString(timestamp));
                if (config.secret() != null && !config.secret().isBlank()) {
                    request = request.header("X-Pkgwatch-Signature", WebhookSigner.sign(config.secret(), timestamp, body));
                }
                request.body(body).retrieve().toBodilessEntity();
                delivered.increment();
                return;
            } catch (RestClientException e) {
                log.warn("webhook attempt {} of {} failed: {}", attempt, MAX_ATTEMPTS, e.getClass().getSimpleName());
                if (attempt < MAX_ATTEMPTS && !pause(200L << (attempt - 1))) {
                    break;
                }
            }
        }
        failed.increment();
    }

    private boolean pause(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
