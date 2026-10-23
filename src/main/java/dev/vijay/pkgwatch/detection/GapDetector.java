package dev.vijay.pkgwatch.detection;

import dev.vijay.pkgwatch.model.UsageReading;

import java.time.Duration;
import java.util.Optional;

public class GapDetector implements AnomalyDetector {

    private final long maxGapSeconds;

    public GapDetector(long maxGapSeconds) {
        this.maxGapSeconds = maxGapSeconds;
    }

    @Override
    public Optional<Anomaly> inspect(SeriesState state, UsageReading reading) {
        if (state.lastTimestamp() == null) {
            return Optional.empty();
        }
        long gapSeconds = Duration.between(state.lastTimestamp(), reading.timestamp()).getSeconds();
        long missing = reading.sequence() - state.lastSequence() - 1;

        if (gapSeconds > maxGapSeconds) {
            Severity severity = gapSeconds > maxGapSeconds * 5 ? Severity.CRITICAL
                    : gapSeconds > maxGapSeconds * 3 ? Severity.HIGH
                    : Severity.MEDIUM;
            String detail = "no data for " + gapSeconds + "s (limit " + maxGapSeconds + "s), "
                    + Math.max(missing, 0) + " readings missing";
            return Optional.of(new Anomaly(AnomalyType.GAP, severity, detail));
        }
        if (missing > 0) {
            Severity severity = missing > 5 ? Severity.MEDIUM : Severity.LOW;
            return Optional.of(new Anomaly(AnomalyType.GAP, severity, missing + " readings missing in sequence"));
        }
        return Optional.empty();
    }
}
