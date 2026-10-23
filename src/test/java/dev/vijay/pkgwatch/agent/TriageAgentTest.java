package dev.vijay.pkgwatch.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vijay.pkgwatch.api.IncidentService;
import dev.vijay.pkgwatch.detection.Anomaly;
import dev.vijay.pkgwatch.detection.AnomalyType;
import dev.vijay.pkgwatch.detection.Severity;
import dev.vijay.pkgwatch.llm.LlmClient;
import dev.vijay.pkgwatch.llm.LlmUnavailableException;
import dev.vijay.pkgwatch.messaging.DlqInspector;
import dev.vijay.pkgwatch.model.UsageReading;
import dev.vijay.pkgwatch.persistence.Incident;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriageAgentTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IncidentService incidents = mock(IncidentService.class);
    private final DlqInspector deadLetters = mock(DlqInspector.class);

    private static final class ScriptedLlm implements LlmClient {
        private final Deque<String> replies = new ArrayDeque<>();
        private final boolean available;

        ScriptedLlm(boolean available, String... replies) {
            this.available = available;
            this.replies.addAll(List.of(replies));
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            if (replies.isEmpty()) {
                throw new LlmUnavailableException("script exhausted");
            }
            return replies.poll();
        }
    }

    private static AgentTool stubTool(String name, String result) {
        return new AgentTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return "stub";
            }

            @Override
            public String execute(JsonNode args) {
                return result;
            }
        };
    }

    private Incident incident(Severity severity) {
        UsageReading reading = new UsageReading("lodash", 3, Instant.parse("2026-01-04T00:00:00Z"), 9000);
        return Incident.open(reading, new Anomaly(AnomalyType.SPIKE, severity, "detail"));
    }

    @Test
    void callsAToolThenReturnsTheModelReport() {
        ScriptedLlm llm = new ScriptedLlm(true,
                "{\"action\":\"tool\",\"tool\":\"get_metrics\",\"args\":{}}",
                "{\"action\":\"final\",\"report\":\"All quiet.\"}");
        TriageAgent agent = new TriageAgent(llm, mapper, List.of(stubTool("get_metrics", "{}")), incidents, deadLetters, 5);

        TriageReport report = agent.run();

        assertEquals("All quiet.", report.report());
        assertEquals(List.of("get_metrics"), report.toolCalls());
        assertTrue(report.usedModel());
        assertEquals(report, agent.latest());
    }

    @Test
    void unknownToolDoesNotBreakTheLoop() {
        ScriptedLlm llm = new ScriptedLlm(true,
                "{\"action\":\"tool\",\"tool\":\"drop_tables\",\"args\":{}}",
                "{\"action\":\"final\",\"report\":\"Nothing to do.\"}");
        TriageAgent agent = new TriageAgent(llm, mapper, List.of(), incidents, deadLetters, 5);

        TriageReport report = agent.run();

        assertEquals("Nothing to do.", report.report());
        assertTrue(report.usedModel());
    }

    @Test
    void usesRulesWhenTheModelIsUnavailable() {
        when(incidents.open()).thenReturn(List.of(incident(Severity.CRITICAL), incident(Severity.LOW)));
        when(deadLetters.recent(10)).thenReturn(List.of());
        TriageAgent agent = new TriageAgent(new ScriptedLlm(false), mapper, List.of(), incidents, deadLetters, 5);

        TriageReport report = agent.run();

        assertFalse(report.usedModel());
        assertTrue(report.report().startsWith("2 open incidents (1 critical, 0 high)"));
    }

    @Test
    void fallsBackToRulesWhenTheModelReturnsGarbage() {
        when(incidents.open()).thenReturn(List.of());
        when(deadLetters.recent(10)).thenReturn(List.of());
        TriageAgent agent = new TriageAgent(new ScriptedLlm(true, "not json"), mapper, List.of(), incidents, deadLetters, 5);

        TriageReport report = agent.run();

        assertFalse(report.usedModel());
        assertTrue(report.report().startsWith("0 open incidents"));
    }

    @Test
    void acknowledgeToolRefusesSevereIncidents() {
        Incident severe = incident(Severity.HIGH);
        when(incidents.find(any(UUID.class))).thenReturn(Optional.of(severe));
        AcknowledgeIncidentTool tool = new AcknowledgeIncidentTool(incidents);

        String result = tool.execute(mapper.createObjectNode().put("id", severe.getId().toString()));

        assertTrue(result.startsWith("refused"));
        verify(incidents, never()).acknowledge(any());
    }

    @Test
    void acknowledgeToolAcceptsMinorIncidents() {
        Incident minor = incident(Severity.LOW);
        when(incidents.find(minor.getId())).thenReturn(Optional.of(minor));
        AcknowledgeIncidentTool tool = new AcknowledgeIncidentTool(incidents);

        String result = tool.execute(mapper.createObjectNode().put("id", minor.getId().toString()));

        assertTrue(result.startsWith("acknowledged"));
        verify(incidents).acknowledge(minor.getId());
    }
}
