package dev.vijay.pkgwatch.producer;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vijay.pkgwatch.config.PkgwatchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NpmStatsClient {

    private static final Logger log = LoggerFactory.getLogger(NpmStatsClient.class);

    private final RestClient http;

    public NpmStatsClient(RestClient.Builder builder, PkgwatchProperties props) {
        this.http = builder.baseUrl(props.producer().npmBaseUrl()).build();
    }

    public List<Double> dailyDownloads(String packageName, LocalDate start, LocalDate end) {
        try {
            JsonNode response = http.get()
                    .uri("/downloads/range/{start}:{end}/{pkg}", start.toString(), end.toString(), packageName)
                    .retrieve()
                    .body(JsonNode.class);
            List<Double> days = new ArrayList<>();
            if (response != null) {
                for (JsonNode day : response.path("downloads")) {
                    days.add(day.path("downloads").asDouble());
                }
            }
            return days;
        } catch (RestClientException e) {
            log.warn("could not fetch download stats for {}: {}", packageName, e.getClass().getSimpleName());
            return List.of();
        }
    }

    public Map<LocalDate, Double> dailyDownloadsByDate(String packageName, LocalDate start, LocalDate end) {
        Map<LocalDate, Double> days = new LinkedHashMap<>();
        try {
            JsonNode response = http.get()
                    .uri("/downloads/range/{start}:{end}/{pkg}", start.toString(), end.toString(), packageName)
                    .retrieve()
                    .body(JsonNode.class);
            if (response != null) {
                for (JsonNode day : response.path("downloads")) {
                    days.put(LocalDate.parse(day.path("day").asText()), day.path("downloads").asDouble());
                }
            }
        } catch (RestClientException | java.time.format.DateTimeParseException e) {
            log.warn("could not fetch download stats for {}: {}", packageName, e.getClass().getSimpleName());
        }
        return days;
    }
}
