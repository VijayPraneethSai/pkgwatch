package dev.vijay.pkgwatch.agent;

import java.time.Instant;
import java.util.List;

public record TriageReport(String report, List<String> toolCalls, boolean usedModel, Instant generatedAt) {
}
