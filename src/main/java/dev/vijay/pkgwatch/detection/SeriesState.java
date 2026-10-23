package dev.vijay.pkgwatch.detection;

import dev.vijay.pkgwatch.model.UsageReading;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SeriesState {

    private final int capacity;
    private final Deque<Double> window = new ArrayDeque<>();
    private final Map<AnomalyType, Long> lastEmitted = new EnumMap<>(AnomalyType.class);
    private Instant lastTimestamp;
    private long lastSequence = -1;

    public SeriesState(int capacity) {
        this.capacity = capacity;
    }

    public List<Double> values() {
        return List.copyOf(window);
    }

    public int size() {
        return window.size();
    }

    public Instant lastTimestamp() {
        return lastTimestamp;
    }

    public long lastSequence() {
        return lastSequence;
    }

    public void add(UsageReading reading) {
        track(reading, true);
    }

    public void track(UsageReading reading, boolean includeInWindow) {
        if (includeInWindow) {
            if (window.size() == capacity) {
                window.removeFirst();
            }
            window.addLast(reading.downloads());
        }
        lastTimestamp = reading.timestamp();
        lastSequence = reading.sequence();
    }

    public boolean cooldownElapsed(AnomalyType type, long sequence, int cooldownReadings) {
        Long last = lastEmitted.get(type);
        return last == null || sequence - last >= cooldownReadings;
    }

    public void markEmitted(AnomalyType type, long sequence) {
        lastEmitted.put(type, sequence);
    }
}
