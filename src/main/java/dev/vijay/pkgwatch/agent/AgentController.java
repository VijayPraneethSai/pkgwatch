package dev.vijay.pkgwatch.agent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/triage")
public class AgentController {

    private final TriageAgent agent;

    public AgentController(TriageAgent agent) {
        this.agent = agent;
    }

    @PostMapping
    public TriageReport run() {
        return agent.run();
    }

    @GetMapping("/latest")
    public ResponseEntity<TriageReport> latest() {
        TriageReport report = agent.latest();
        return report == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(report);
    }
}
