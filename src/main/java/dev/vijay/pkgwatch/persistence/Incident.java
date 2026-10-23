package dev.vijay.pkgwatch.persistence;

import dev.vijay.pkgwatch.detection.Anomaly;
import dev.vijay.pkgwatch.detection.AnomalyType;
import dev.vijay.pkgwatch.detection.Severity;
import dev.vijay.pkgwatch.model.UsageReading;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents", indexes = @Index(name = "idx_incident_status", columnList = "status"))
public class Incident {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String packageName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Column(length = 1000)
    private String detail;

    @Column(length = 2000)
    private String summary;

    @Column(length = 1000)
    private String likelyCause;

    private double observedValue;

    private Instant observedAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant acknowledgedAt;

    protected Incident() {
    }

    public static Incident open(UsageReading reading, Anomaly anomaly) {
        Incident incident = new Incident();
        incident.id = UUID.randomUUID();
        incident.packageName = reading.packageName();
        incident.type = anomaly.type();
        incident.severity = anomaly.severity();
        incident.status = IncidentStatus.OPEN;
        incident.detail = anomaly.detail();
        incident.observedValue = reading.downloads();
        incident.observedAt = reading.timestamp();
        incident.createdAt = Instant.now();
        return incident;
    }

    public void describe(String summary, String likelyCause) {
        this.summary = summary;
        this.likelyCause = likelyCause;
    }

    public void acknowledge() {
        this.status = IncidentStatus.ACKNOWLEDGED;
        this.acknowledgedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getPackageName() {
        return packageName;
    }

    public AnomalyType getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public String getSummary() {
        return summary;
    }

    public String getLikelyCause() {
        return likelyCause;
    }

    public double getObservedValue() {
        return observedValue;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }
}
