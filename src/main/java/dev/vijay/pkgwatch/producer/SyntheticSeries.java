package dev.vijay.pkgwatch.producer;

import java.util.Random;

final class SyntheticSeries {

    private SyntheticSeries() {
    }

    static double[] generate(String packageName, int days) {
        Random random = new Random(packageName.hashCode());
        double base = 20_000 + Math.abs(packageName.hashCode() % 80_000);
        int spikeDay = (int) (days * 0.55);
        int driftStart = (int) (days * 0.75);

        double[] values = new double[days];
        for (int i = 0; i < days; i++) {
            boolean weekend = i % 7 == 5 || i % 7 == 6;
            double value = base * (weekend ? 0.65 : 1.0) * (1 + random.nextGaussian() * 0.04);
            if (i == spikeDay) {
                value *= 4;
            }
            if (i >= driftStart) {
                value *= 1 + Math.min(0.4, 0.01 * (i - driftStart));
            }
            values[i] = Math.max(0, Math.round(value));
        }
        return values;
    }
}
