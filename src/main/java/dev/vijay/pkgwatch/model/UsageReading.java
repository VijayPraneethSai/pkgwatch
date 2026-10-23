package dev.vijay.pkgwatch.model;

import java.time.Instant;

public record UsageReading(
        String packageName,
        long sequence,
        Instant timestamp,
        double downloads) {

    public String eventId() {
        return packageName + ":" + sequence;
    }

    public void validate() {
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("packageName is required");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
        if (Double.isNaN(downloads) || Double.isInfinite(downloads) || downloads < 0) {
            throw new IllegalArgumentException("downloads must be a non-negative number");
        }
    }
}
