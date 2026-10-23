package dev.vijay.pkgwatch.detection;

public enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public boolean requiresHuman() {
        return this == HIGH || this == CRITICAL;
    }
}
