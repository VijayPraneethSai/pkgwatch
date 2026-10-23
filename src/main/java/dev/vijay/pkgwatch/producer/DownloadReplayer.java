package dev.vijay.pkgwatch.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vijay.pkgwatch.config.PkgwatchProperties;
import dev.vijay.pkgwatch.model.UsageReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@ConditionalOnProperty(prefix = "pkgwatch.producer", name = "enabled", havingValue = "true")
public class DownloadReplayer {

    private static final Logger log = LoggerFactory.getLogger(DownloadReplayer.class);

    private final KafkaTemplate<String, String> kafka;
    private final PkgwatchProperties props;
    private final ObjectMapper mapper;
    private final NpmStatsClient npm;
    private final Random random = new Random(42);
    private final Map<String, double[]> series = new LinkedHashMap<>();

    private LocalDate startDate;
    private int length;
    private int cursor;
    private boolean loaded;
    private boolean finished;

    public DownloadReplayer(KafkaTemplate<String, String> kafka, PkgwatchProperties props,
                            ObjectMapper mapper, NpmStatsClient npm) {
        this.kafka = kafka;
        this.props = props;
        this.mapper = mapper;
        this.npm = npm;
    }

    @Scheduled(fixedDelayString = "${pkgwatch.producer.interval-ms:300}")
    public void tick() {
        if (finished) {
            return;
        }
        if (!loaded) {
            load();
        }
        if (cursor >= length) {
            finished = true;
            log.info("replay finished after {} days", length);
            return;
        }
        for (Map.Entry<String, double[]> entry : series.entrySet()) {
            publish(entry.getKey(), cursor, entry.getValue()[cursor]);
        }
        cursor++;
    }

    private void load() {
        PkgwatchProperties.Producer config = props.producer();
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(2);
        startDate = end.minusDays(config.days() - 1L);
        length = Integer.MAX_VALUE;
        for (String name : config.packages()) {
            List<Double> days = npm.dailyDownloads(name, startDate, end);
            double[] values;
            if (days.size() < 30) {
                log.warn("using generated data for {}", name);
                values = SyntheticSeries.generate(name, config.days());
            } else {
                values = days.stream().mapToDouble(Double::doubleValue).toArray();
            }
            series.put(name, values);
            length = Math.min(length, values.length);
        }
        loaded = true;
        log.info("replaying {} days for {} packages", length, series.size());
    }

    private void publish(String packageName, int sequence, double downloads) {
        Instant timestamp = startDate.plusDays(sequence).atStartOfDay(ZoneOffset.UTC).toInstant();
        String payload;
        try {
            payload = mapper.writeValueAsString(new UsageReading(packageName, sequence, timestamp, downloads));
        } catch (JsonProcessingException e) {
            log.warn("skipping unserializable reading for {}", packageName);
            return;
        }
        String topic = props.topics().downloads();
        if (props.producer().faultInjection()) {
            double roll = random.nextDouble();
            if (roll < 0.01) {
                kafka.send(topic, packageName, "{\"packageName\":\"" + packageName + "\",\"sequence\":");
            } else if (roll < 0.02) {
                kafka.send(topic, packageName, payload);
            } else if (roll < 0.03) {
                return;
            }
        }
        kafka.send(topic, packageName, payload);
    }
}
