package dev.vijay.pkgwatch.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status);

    long countByStatus(IncidentStatus status);
}
