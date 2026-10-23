package dev.vijay.pkgwatch.api;

import dev.vijay.pkgwatch.persistence.Incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentView(
        UUID id,
        String packageName,
        String type,
        String severity,
        String status,
        String detail,
        String summary,
        String likelyCause,
        double observedValue,
        Instant observedAt,
        Instant createdAt,
        Instant acknowledgedAt) {

    public static IncidentView from(Incident incident) {
        return new IncidentView(
                incident.getId(),
                incident.getPackageName(),
                incident.getType().name(),
                incident.getSeverity().name(),
                incident.getStatus().name(),
                incident.getDetail(),
                incident.getSummary(),
                incident.getLikelyCause(),
                incident.getObservedValue(),
                incident.getObservedAt(),
                incident.getCreatedAt(),
                incident.getAcknowledgedAt());
    }
}
