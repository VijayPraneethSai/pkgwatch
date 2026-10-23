package dev.vijay.pkgwatch.agent;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vijay.pkgwatch.api.IncidentService;
import dev.vijay.pkgwatch.persistence.Incident;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AcknowledgeIncidentTool implements AgentTool {

    private final IncidentService incidents;

    public AcknowledgeIncidentTool(IncidentService incidents) {
        this.incidents = incidents;
    }

    @Override
    public String name() {
        return "acknowledge_incident";
    }

    @Override
    public String description() {
        return "Acknowledges a LOW or MEDIUM incident. Argument: {\"id\": \"<incident id>\"}. HIGH and CRITICAL need a human.";
    }

    @Override
    public String execute(JsonNode args) {
        UUID id;
        try {
            id = UUID.fromString(args.path("id").asText(""));
        } catch (IllegalArgumentException e) {
            return "error: id must be a valid incident id";
        }
        Optional<Incident> found = incidents.find(id);
        if (found.isEmpty()) {
            return "error: no incident with that id";
        }
        if (found.get().getSeverity().requiresHuman()) {
            return "refused: HIGH and CRITICAL incidents need human acknowledgement";
        }
        incidents.acknowledge(id);
        return "acknowledged " + id;
    }
}
