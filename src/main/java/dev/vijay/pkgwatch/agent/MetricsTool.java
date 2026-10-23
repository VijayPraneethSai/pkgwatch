package dev.vijay.pkgwatch.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MetricsTool implements AgentTool {

    private static final String[] COUNTERS = {
            "pkgwatch.readings.processed",
            "pkgwatch.readings.duplicates",
            "pkgwatch.dlq.published",
            "pkgwatch.llm.fallbacks",
            "pkgwatch.webhook.delivered",
            "pkgwatch.webhook.failed"
    };

    private final MeterRegistry registry;
    private final ObjectMapper mapper;

    public MetricsTool(MeterRegistry registry, ObjectMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "get_metrics";
    }

    @Override
    public String description() {
        return "Returns pipeline counters: readings processed, duplicates, dead letters, model fallbacks, webhook results. No arguments.";
    }

    @Override
    public String execute(JsonNode args) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (String counterName : COUNTERS) {
            Counter counter = registry.find(counterName).counter();
            values.put(counterName, counter == null ? 0L : (long) counter.count());
        }
        try {
            return mapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return "error: could not serialize metrics";
        }
    }
}
