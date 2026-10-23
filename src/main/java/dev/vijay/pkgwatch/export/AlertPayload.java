package dev.vijay.pkgwatch.export;

import dev.vijay.pkgwatch.persistence.Incident;

import java.time.Instant;

public record AlertPayload(
        String incidentId,
        String packageName,
        String type,
        String severity,
        String summary,
        String likelyCause,
        Instant detectedAt) {

    public static AlertPayload from(Incident incident) {
        return new AlertPayload(
                incident.getId().toString(),
                incident.getPackageName(),
                incident.getType().name(),
                incident.getSeverity().name(),
                incident.getSummary(),
                incident.getLikelyCause(),
                incident.getCreatedAt());
    }
}
