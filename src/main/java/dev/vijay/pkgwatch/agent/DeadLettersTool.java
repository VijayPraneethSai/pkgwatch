package dev.vijay.pkgwatch.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vijay.pkgwatch.messaging.DlqInspector;
import org.springframework.stereotype.Component;

@Component
public class DeadLettersTool implements AgentTool {

    private final DlqInspector inspector;
    private final ObjectMapper mapper;

    public DeadLettersTool(DlqInspector inspector, ObjectMapper mapper) {
        this.inspector = inspector;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "list_dead_letters";
    }

    @Override
    public String description() {
        return "Lists the 10 most recent messages sent to the dead letter topic with the error that caused it. No arguments.";
    }

    @Override
    public String execute(JsonNode args) {
        try {
            return mapper.writeValueAsString(inspector.recent(10));
        } catch (JsonProcessingException e) {
            return "error: could not serialize dead letters";
        }
    }
}
