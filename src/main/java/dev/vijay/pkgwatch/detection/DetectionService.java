package dev.vijay.pkgwatch.detection;

import dev.vijay.pkgwatch.config.PkgwatchProperties;
import dev.vijay.pkgwatch.model.UsageReading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DetectionService {

    private final PkgwatchProperties.Detection config;
    private final List<AnomalyDetector> detectors;
    private final Map<String, SeriesState> states = new ConcurrentHashMap<>();

    @Autowired
    public DetectionService(PkgwatchProperties props) {
        this(props.detection());
    }

    public DetectionService(PkgwatchProperties.Detection config) {
        this.config = config;
        this.detectors = List.of(
                new ZScoreSpikeDetector(config.minSamples(), config.spikeZScore()),
                new DriftDetector(config.windowSize(), config.driftSigma()),
                new GapDetector(config.maxGapSeconds()));
    }

    public List<Anomaly> evaluate(UsageReading reading) {
        SeriesState state = states.computeIfAbsent(reading.packageName(), name -> new SeriesState(config.windowSize()));
        synchronized (state) {
            List<Anomaly> emitted = new ArrayList<>();
            boolean spike = false;
            for (AnomalyDetector detector : detectors) {
                Optional<Anomaly> found = detector.inspect(state, reading);
                if (found.isPresent()) {
                    Anomaly anomaly = found.get();
                    if (anomaly.type() == AnomalyType.SPIKE) {
                        spike = true;
                    }
                    if (state.cooldownElapsed(anomaly.type(), reading.sequence(), config.cooldownReadings())) {
                        state.markEmitted(anomaly.type(), reading.sequence());
                        emitted.add(anomaly);
                    }
                }
            }
            state.track(reading, !spike);
            return emitted;
        }
    }

    public int trackedSeries() {
        return states.size();
    }
}
