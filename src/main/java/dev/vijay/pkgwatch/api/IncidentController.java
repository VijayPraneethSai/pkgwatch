package dev.vijay.pkgwatch.api;

import dev.vijay.pkgwatch.persistence.Incident;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService service;

    public IncidentController(IncidentService service) {
        this.service = service;
    }

    @GetMapping
    public List<IncidentView> list(@RequestParam(defaultValue = "false") boolean openOnly,
                                   @RequestParam(defaultValue = "100") int limit) {
        List<Incident> incidents = openOnly ? service.open() : service.recent(Math.min(Math.max(limit, 1), 500));
        return incidents.stream().map(IncidentView::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentView> get(@PathVariable UUID id) {
        return service.find(id).map(IncidentView::from).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<IncidentView> acknowledge(@PathVariable UUID id) {
        return service.acknowledge(id).map(IncidentView::from).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
