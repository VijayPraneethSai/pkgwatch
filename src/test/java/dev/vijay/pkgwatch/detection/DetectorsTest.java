package dev.vijay.pkgwatch.detection;

import dev.vijay.pkgwatch.model.UsageReading;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorsTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static UsageReading reading(long sequence, long offsetSeconds, double downloads) {
        return new UsageReading("lodash", sequence, T0.plusSeconds(offsetSeconds), downloads);
    }

    private static SeriesState fill(int count, double center, double amplitude) {
        SeriesState state = new SeriesState(40);
        for (int i = 0; i < count; i++) {
            state.add(reading(i, i, center + (i % 2 == 0 ? amplitude : -amplitude)));
        }
        return state;
    }

    @Test
    void spikeIsFlaggedWhenFarFromRollingMean() {
        SeriesState state = fill(20, 1000.0, 10.0);
        Optional<Anomaly> result = new ZScoreSpikeDetector(10, 4.0).inspect(state, reading(20, 20, 1100.0));
        assertTrue(result.isPresent());
        assertEquals(AnomalyType.SPIKE, result.get().type());
        assertEquals(Severity.CRITICAL, result.get().severity());
    }

    @Test
    void normalValueIsNotASpike() {
        SeriesState state = fill(20, 1000.0, 10.0);
        assertTrue(new ZScoreSpikeDetector(10, 4.0).inspect(state, reading(20, 20, 1005.0)).isEmpty());
    }

    @Test
    void spikeNeedsEnoughSamples() {
        SeriesState state = fill(5, 1000.0, 10.0);
        assertTrue(new ZScoreSpikeDetector(10, 4.0).inspect(state, reading(5, 5, 5000.0)).isEmpty());
    }

    @Test
    void driftIsFlaggedWhenRecentMeanMoves() {
        SeriesState state = new SeriesState(40);
        for (int i = 0; i < 20; i++) {
            state.add(reading(i, i, 1000.0 + (i % 2 == 0 ? 10.0 : -10.0)));
        }
        for (int i = 20; i < 40; i++) {
            state.add(reading(i, i, 1060.0 + (i % 2 == 0 ? 10.0 : -10.0)));
        }
        Optional<Anomaly> result = new DriftDetector(40, 2.5).inspect(state, reading(40, 40, 1060.0));
        assertTrue(result.isPresent());
        assertEquals(AnomalyType.DRIFT, result.get().type());
        assertEquals(Severity.HIGH, result.get().severity());
    }

    @Test
    void stableSeriesIsNotDrift() {
        SeriesState state = fill(40, 1000.0, 10.0);
        assertTrue(new DriftDetector(40, 2.5).inspect(state, reading(40, 40, 1000.0)).isEmpty());
    }

    @Test
    void longSilenceIsAGap() {
        SeriesState state = new SeriesState(40);
        state.add(reading(5, 0, 1000.0));
        Optional<Anomaly> result = new GapDetector(30).inspect(state, reading(6, 120, 1000.0));
        assertTrue(result.isPresent());
        assertEquals(AnomalyType.GAP, result.get().type());
        assertEquals(Severity.HIGH, result.get().severity());
    }

    @Test
    void skippedSequenceNumbersAreALowSeverityGap() {
        SeriesState state = new SeriesState(40);
        state.add(reading(5, 0, 1000.0));
        Optional<Anomaly> result = new GapDetector(30).inspect(state, reading(10, 1, 1000.0));
        assertTrue(result.isPresent());
        assertEquals(Severity.LOW, result.get().severity());
    }

    @Test
    void firstReadingAndSteadyStreamHaveNoGap() {
        GapDetector detector = new GapDetector(30);
        SeriesState state = new SeriesState(40);
        assertTrue(detector.inspect(state, reading(0, 0, 1000.0)).isEmpty());
        state.add(reading(0, 0, 1000.0));
        assertTrue(detector.inspect(state, reading(1, 1, 1000.0)).isEmpty());
    }
}
