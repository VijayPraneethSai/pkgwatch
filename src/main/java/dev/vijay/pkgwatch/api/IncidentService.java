package dev.vijay.pkgwatch.api;

import dev.vijay.pkgwatch.persistence.Incident;
import dev.vijay.pkgwatch.persistence.IncidentRepository;
import dev.vijay.pkgwatch.persistence.IncidentStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentRepository repository;

    public IncidentService(IncidentRepository repository) {
        this.repository = repository;
    }

    public List<Incident> open() {
        List<Incident> incidents = new ArrayList<>(repository.findByStatusOrderByCreatedAtDesc(IncidentStatus.OPEN));
        incidents.sort(Comparator.comparing(Incident::getSeverity).reversed()
                .thenComparing(Incident::getCreatedAt, Comparator.reverseOrder()));
        return incidents;
    }

    public List<Incident> recent(int limit) {
        return repository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    public Optional<Incident> find(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public Optional<Incident> acknowledge(UUID id) {
        return repository.findById(id).map(incident -> {
            incident.acknowledge();
            return repository.save(incident);
        });
    }
}
