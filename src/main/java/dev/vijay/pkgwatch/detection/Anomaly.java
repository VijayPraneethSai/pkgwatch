package dev.vijay.pkgwatch.detection;

public record Anomaly(AnomalyType type, Severity severity, String detail) {
}
