package dev.vijay.pkgwatch.detection;

import dev.vijay.pkgwatch.model.UsageReading;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ZScoreSpikeDetector implements AnomalyDetector {

    private final int minSamples;
    private final double threshold;

    public ZScoreSpikeDetector(int minSamples, double threshold) {
        this.minSamples = minSamples;
        this.threshold = threshold;
    }

    @Override
    public Optional<Anomaly> inspect(SeriesState state, UsageReading reading) {
        if (state.size() < minSamples) {
            return Optional.empty();
        }
        List<Double> values = state.values();
        double mean = Stats.mean(values);
        double sd = Stats.stdDev(values, mean);
        if (sd < 1e-9) {
            return Optional.empty();
        }
        double z = Math.abs(reading.downloads() - mean) / sd;
        if (z < threshold) {
            return Optional.empty();
        }
        Severity severity = z >= threshold * 2 ? Severity.CRITICAL
                : z >= threshold * 1.5 ? Severity.HIGH
                : Severity.MEDIUM;
        String detail = String.format(Locale.ROOT,
                "%.0f downloads is %.1f standard deviations from the rolling mean of %.0f",
                reading.downloads(), z, mean);
        return Optional.of(new Anomaly(AnomalyType.SPIKE, severity, detail));
    }
}
