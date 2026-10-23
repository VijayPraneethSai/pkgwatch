package dev.vijay.pkgwatch.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vijay.pkgwatch.api.IncidentService;
import dev.vijay.pkgwatch.persistence.Incident;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenIncidentsTool implements AgentTool {

    private static final int LIMIT = 10;

    private final IncidentService incidents;
    private final ObjectMapper mapper;

    public OpenIncidentsTool(IncidentService incidents, ObjectMapper mapper) {
        this.incidents = incidents;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "list_open_incidents";
    }

    @Override
    public String description() {
        return "Lists up to 10 open incidents, most severe first, with id, package, type, severity and summary. No arguments.";
    }

    @Override
    public String execute(JsonNode args) {
        List<Map<String, Object>> rows = incidents.open().stream().limit(LIMIT).map(this::row).toList();
        try {
            return mapper.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            return "error: could not serialize incidents";
        }
    }

    private Map<String, Object> row(Incident incident) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", incident.getId().toString());
        row.put("package", incident.getPackageName());
        row.put("type", incident.getType().name());
        row.put("severity", incident.getSeverity().name());
        row.put("summary", incident.getSummary());
        return row;
    }
}
