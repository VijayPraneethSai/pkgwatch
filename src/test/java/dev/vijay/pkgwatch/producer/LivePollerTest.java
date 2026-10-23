package dev.vijay.pkgwatch.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vijay.pkgwatch.config.PkgwatchProperties;
import dev.vijay.pkgwatch.model.UsageReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LivePollerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC);

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private KafkaTemplate<String, String> kafka;
    private NpmStatsClient npm;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafka = mock(KafkaTemplate.class);
        npm = mock(NpmStatsClient.class);
    }

    private LivePoller poller(List<String> packages) {
        PkgwatchProperties props = new PkgwatchProperties(
                new PkgwatchProperties.Topics("package.downloads", 3),
                new PkgwatchProperties.Detection(40, 10, 4.0, 2.5, 172800, 14),
                new PkgwatchProperties.Llm("", "model", "http://localhost", 1),
                new PkgwatchProperties.Webhook("", ""),
                new PkgwatchProperties.Producer(false, List.of(), 365, 300, false, "http://localhost"),
                new PkgwatchProperties.Live(true, "", packages, 5, 1000),
                new PkgwatchProperties.Agent(false, 1000, 1));
        return new LivePoller(kafka, props, mapper, npm, CLOCK);
    }

    @Test
    void publishesOneReadingPerDayWithStableSequence() throws Exception {
        Map<LocalDate, Double> days = new LinkedHashMap<>();
        days.put(LocalDate.parse("2026-10-21"), 100.0);
        days.put(LocalDate.parse("2026-10-22"), 120.0);
        when(npm.dailyDownloadsByDate(eq("react"), any(), any())).thenReturn(days);

        poller(List.of("react")).poll();

        ArgumentCaptor<String> payloads = ArgumentCaptor.forClass(String.class);
        verify(kafka, times(2)).send(eq("package.downloads"), eq("react"), payloads.capture());
        UsageReading first = mapper.readValue(payloads.getAllValues().get(0), UsageReading.class);
        assertEquals(LocalDate.parse("2026-10-21").toEpochDay(), first.sequence());
        assertEquals(100.0, first.downloads());
    }

    @Test
    void firstPollBackfillsAndLaterPollsOnlyRefresh() {
        LivePoller poller = poller(List.of("react"));
        when(npm.dailyDownloadsByDate(eq("react"), any(), any())).thenReturn(Map.of());

        poller.poll();
        poller.poll();

        ArgumentCaptor<LocalDate> starts = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> ends = ArgumentCaptor.forClass(LocalDate.class);
        verify(npm, times(2)).dailyDownloadsByDate(eq("react"), starts.capture(), ends.capture());
        assertEquals(LocalDate.parse("2026-10-22"), ends.getAllValues().get(0));
        assertEquals(LocalDate.parse("2026-10-18"), starts.getAllValues().get(0));
        assertEquals(LocalDate.parse("2026-10-19"), starts.getAllValues().get(1));
    }

    @Test
    void emptyWatchlistDoesNothing() {
        poller(List.of()).poll();

        verify(npm, never()).dailyDownloadsByDate(any(), any(), any());
        verify(kafka, never()).send(any(String.class), any(String.class), any(String.class));
    }
}
