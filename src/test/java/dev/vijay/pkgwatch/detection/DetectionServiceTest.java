package dev.vijay.pkgwatch.detection;

import dev.vijay.pkgwatch.config.PkgwatchProperties;
import dev.vijay.pkgwatch.model.UsageReading;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final DetectionService service =
            new DetectionService(new PkgwatchProperties.Detection(40, 10, 4.0, 2.5, 30, 20));

    private UsageReading reading(String pkg, long sequence, double downloads) {
        return new UsageReading(pkg, sequence, T0.plusSeconds(sequence), downloads);
    }

    private void warmUp(String pkg, int count) {
        for (int i = 0; i < count; i++) {
            List<Anomaly> anomalies = service.evaluate(reading(pkg, i, 1000.0 + (i % 2 == 0 ? 10.0 : -10.0)));
            assertTrue(anomalies.isEmpty());
        }
    }

    @Test
    void spikeIsReportedOnce() {
        warmUp("lodash", 20);
        List<Anomaly> first = service.evaluate(reading("lodash", 20, 5000.0));
        assertEquals(1, first.size());
        assertEquals(AnomalyType.SPIKE, first.get(0).type());
    }

    @Test
    void repeatedSpikesInsideCooldownAreSuppressed() {
        warmUp("lodash", 20);
        assertEquals(1, service.evaluate(reading("lodash", 20, 5000.0)).size());
        assertTrue(service.evaluate(reading("lodash", 21, 5000.0)).isEmpty());
    }

    @Test
    void packagesAreTrackedIndependently() {
        warmUp("lodash", 20);
        warmUp("express", 20);
        assertEquals(1, service.evaluate(reading("lodash", 20, 5000.0)).size());
        assertTrue(service.evaluate(reading("express", 20, 1005.0)).isEmpty());
        assertEquals(2, service.trackedSeries());
    }
}
