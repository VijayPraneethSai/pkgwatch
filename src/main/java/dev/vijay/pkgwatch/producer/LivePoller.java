package dev.vijay.pkgwatch.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vijay.pkgwatch.config.PkgwatchProperties;
import dev.vijay.pkgwatch.model.UsageReading;
import dev.vijay.pkgwatch.watch.Watchlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "pkgwatch.live", name = "enabled", havingValue = "true")
public class LivePoller {

    private static final Logger log = LoggerFactory.getLogger(LivePoller.class);
    private static final int REFRESH_DAYS = 4;
    private static final int PUBLISH_LAG_DAYS = 2;

    private final KafkaTemplate<String, String> kafka;
    private final PkgwatchProperties props;
    private final ObjectMapper mapper;
    private final NpmStatsClient npm;
    private final Clock clock;
    private boolean backfilled;

    @Autowired
    public LivePoller(KafkaTemplate<String, String> kafka, PkgwatchProperties props,
                      ObjectMapper mapper, NpmStatsClient npm) {
        this(kafka, props, mapper, npm, Clock.systemUTC());
    }

    LivePoller(KafkaTemplate<String, String> kafka, PkgwatchProperties props,
               ObjectMapper mapper, NpmStatsClient npm, Clock clock) {
        this.kafka = kafka;
        this.props = props;
        this.mapper = mapper;
        this.npm = npm;
        this.clock = clock;
    }

    @Scheduled(initialDelay = 5000, fixedDelayString = "${pkgwatch.live.interval-ms:21600000}")
    public void poll() {
        PkgwatchProperties.Live live = props.live();
        Set<String> watched;
        try {
            String file = live.packageJson();
            watched = Watchlist.load(file.isBlank() ? null : Path.of(file), live.packages(), mapper);
        } catch (IOException e) {
            log.warn("could not read the watchlist: {}", e.getMessage());
            return;
        }
        if (watched.isEmpty()) {
            log.warn("live polling is enabled but nothing is on the watchlist");
            return;
        }
        LocalDate end = LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(PUBLISH_LAG_DAYS);
        LocalDate start = end.minusDays((backfilled ? REFRESH_DAYS : live.backfillDays()) - 1L);
        int sent = 0;
        for (String name : watched) {
            for (Map.Entry<LocalDate, Double> day : npm.dailyDownloadsByDate(name, start, end).entrySet()) {
                sent += publish(name, day.getKey(), day.getValue()) ? 1 : 0;
            }
        }
        backfilled = true;
        log.info("published {} readings for {} packages", sent, watched.size());
    }

    private boolean publish(String packageName, LocalDate day, double downloads) {
        UsageReading reading = new UsageReading(packageName, day.toEpochDay(),
                day.atStartOfDay(ZoneOffset.UTC).toInstant(), downloads);
        try {
            kafka.send(props.topics().downloads(), packageName, mapper.writeValueAsString(reading));
            return true;
        } catch (JsonProcessingException e) {
            log.warn("skipping unserializable reading for {}", packageName);
            return false;
        }
    }
}
