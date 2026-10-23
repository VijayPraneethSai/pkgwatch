package dev.vijay.pkgwatch.detection;

import dev.vijay.pkgwatch.model.UsageReading;

import java.util.Optional;

public interface AnomalyDetector {

    Optional<Anomaly> inspect(SeriesState state, UsageReading reading);
}
