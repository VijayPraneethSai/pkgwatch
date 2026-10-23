package dev.vijay.pkgwatch.agent;

import com.fasterxml.jackson.databind.JsonNode;

public interface AgentTool {

    String name();

    String description();

    String execute(JsonNode args);
}
