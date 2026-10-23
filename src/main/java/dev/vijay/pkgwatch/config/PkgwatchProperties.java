package dev.vijay.pkgwatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "pkgwatch")
public record PkgwatchProperties(
        @DefaultValue Topics topics,
        @DefaultValue Detection detection,
        @DefaultValue Llm llm,
        @DefaultValue Webhook webhook,
        @DefaultValue Producer producer,
        @DefaultValue Agent agent) {

    public record Topics(
            @DefaultValue("package.downloads") String downloads,
            @DefaultValue("3") int partitions) {

        public String dlq() {
            return downloads + ".dlq";
        }
    }

    public record Detection(
            @DefaultValue("40") int windowSize,
            @DefaultValue("10") int minSamples,
            @DefaultValue("4.0") double spikeZScore,
            @DefaultValue("2.5") double driftSigma,
            @DefaultValue("172800") long maxGapSeconds,
            @DefaultValue("14") int cooldownReadings) {
    }

    public record Llm(
            @DefaultValue("") String apiKey,
            @DefaultValue("gemini-2.5-flash") String model,
            @DefaultValue("https://generativelanguage.googleapis.com") String baseUrl,
            @DefaultValue("10") long timeoutSeconds) {
    }

    public record Webhook(
            @DefaultValue("") String url,
            @DefaultValue("") String secret) {
    }

    public record Producer(
            @DefaultValue("false") boolean enabled,
            @DefaultValue({"lodash", "express", "react", "axios"}) List<String> packages,
            @DefaultValue("365") int days,
            @DefaultValue("300") long intervalMs,
            @DefaultValue("true") boolean faultInjection,
            @DefaultValue("https://api.npmjs.org") String npmBaseUrl) {
    }

    public record Agent(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("300000") long intervalMs,
            @DefaultValue("5") int maxSteps) {
    }
}
