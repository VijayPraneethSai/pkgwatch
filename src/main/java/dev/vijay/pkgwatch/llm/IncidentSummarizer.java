package dev.vijay.pkgwatch.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vijay.pkgwatch.detection.Anomaly;
import dev.vijay.pkgwatch.model.UsageReading;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class IncidentSummarizer {

    private static final Logger log = LoggerFactory.getLogger(IncidentSummarizer.class);

    private static final String SYSTEM_PROMPT = """
            You help maintainers read download statistics for open-source packages.
            Given one detected anomaly in a package's daily download count, reply with a JSON object
            with exactly two string fields:
            "summary" (one or two plain sentences) and
            "likelyCause" (one short sentence, hedged, never stated as fact).
            Use only the data provided.""";

    private final LlmClient llm;
    private final ObjectMapper mapper;
    private final Timer latency;
    private final Counter fallbacks;

    public IncidentSummarizer(LlmClient llm, ObjectMapper mapper, MeterRegistry registry) {
        this.llm = llm;
        this.mapper = mapper;
        this.latency = Timer.builder("pkgwatch.llm.latency").register(registry);
        this.fallbacks = Counter.builder("pkgwatch.llm.fallbacks").register(registry);
    }

    public IncidentSummary summarize(UsageReading reading, Anomaly anomaly) {
        if (!llm.isAvailable()) {
            fallbacks.increment();
            return fallback(reading, anomaly);
        }
        Timer.Sample sample = Timer.start();
        try {
            String raw = llm.complete(SYSTEM_PROMPT, userPrompt(reading, anomaly));
            JsonNode node = mapper.readTree(raw);
            String summary = node.path("summary").asText("");
            String cause = node.path("likelyCause").asText("");
            if (summary.isBlank()) {
                throw new IllegalStateException("model response had no summary");
            }
            return new IncidentSummary(truncate(summary, 500), truncate(cause, 300));
        } catch (Exception e) {
            log.warn("summary generation failed, using template: {}", e.getMessage());
            fallbacks.increment();
            return fallback(reading, anomaly);
        } finally {
            sample.stop(latency);
        }
    }

    private String userPrompt(UsageReading reading, Anomaly anomaly) {
        return String.format(Locale.ROOT,
                "package=%s type=%s severity=%s downloads=%.0f detail=%s",
                reading.packageName(), anomaly.type(), anomaly.severity(), reading.downloads(), anomaly.detail());
    }

    private IncidentSummary fallback(UsageReading reading, Anomaly anomaly) {
        String summary = String.format(Locale.ROOT, "Package %s showed a %s anomaly (%s severity): %s.",
                reading.packageName(), anomaly.type(), anomaly.severity(), anomaly.detail());
        return new IncidentSummary(summary, "Not determined");
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
