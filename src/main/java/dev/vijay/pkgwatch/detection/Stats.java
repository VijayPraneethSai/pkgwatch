package dev.vijay.pkgwatch.detection;

import java.util.List;

final class Stats {

    private Stats() {
    }

    static double mean(List<Double> values) {
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return values.isEmpty() ? 0 : sum / values.size();
    }

    static double stdDev(List<Double> values, double mean) {
        if (values.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (double v : values) {
            sum += (v - mean) * (v - mean);
        }
        return Math.sqrt(sum / values.size());
    }
}
