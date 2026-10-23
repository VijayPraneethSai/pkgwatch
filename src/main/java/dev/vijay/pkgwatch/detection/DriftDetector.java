package dev.vijay.pkgwatch.detection;

import dev.vijay.pkgwatch.model.UsageReading;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DriftDetector implements AnomalyDetector {

    private final int windowSize;
    private final double sigma;

    public DriftDetector(int windowSize, double sigma) {
        this.windowSize = windowSize;
        this.sigma = sigma;
    }

    @Override
    public Optional<Anomaly> inspect(SeriesState state, UsageReading reading) {
        if (state.size() < windowSize) {
            return Optional.empty();
        }
        List<Double> values = state.values();
        int half = values.size() / 2;
        List<Double> older = values.subList(0, half);
        List<Double> newer = values.subList(half, values.size());

        double olderMean = Stats.mean(older);
        double newerMean = Stats.mean(newer);
        double olderSd = Math.max(Stats.stdDev(older, olderMean), 1e-6);

        double shift = Math.abs(newerMean - olderMean);
        double limit = sigma * olderSd;
        if (shift <= limit) {
            return Optional.empty();
        }
        Severity severity = shift / limit >= 2 ? Severity.HIGH : Severity.MEDIUM;
        String detail = String.format(Locale.ROOT,
                "average moved from %.0f to %.0f across the rolling window (%.1f sigma of the earlier half)",
                olderMean, newerMean, shift / olderSd);
        return Optional.of(new Anomaly(AnomalyType.DRIFT, severity, detail));
    }
}
