package dev.vijay.pkgwatch.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pkgwatch.agent", name = "enabled", havingValue = "true")
public class AgentScheduler {

    private final TriageAgent agent;

    public AgentScheduler(TriageAgent agent) {
        this.agent = agent;
    }

    @Scheduled(fixedDelayString = "${pkgwatch.agent.interval-ms:300000}", initialDelayString = "${pkgwatch.agent.interval-ms:300000}")
    public void triage() {
        agent.run();
    }
}
