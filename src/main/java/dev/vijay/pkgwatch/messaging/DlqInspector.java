package dev.vijay.pkgwatch.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class DlqInspector {

    public record DlqEntry(Instant receivedAt, String key, String payload, String error) {
    }

    private static final int MAX_ENTRIES = 50;
    private static final int MAX_PAYLOAD = 300;

    private final Deque<DlqEntry> recent = new ConcurrentLinkedDeque<>();

    @KafkaListener(id = "pkgwatch-dlq-inspector", topics = "${pkgwatch.topics.downloads}.dlq")
    public void onDeadLetter(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE);
        String error = header == null ? "unknown" : new String(header.value(), StandardCharsets.UTF_8);
        String payload = record.value() == null ? "" : record.value();
        if (payload.length() > MAX_PAYLOAD) {
            payload = payload.substring(0, MAX_PAYLOAD);
        }
        recent.addFirst(new DlqEntry(Instant.now(), record.key(), payload, error));
        while (recent.size() > MAX_ENTRIES) {
            recent.pollLast();
        }
    }

    public List<DlqEntry> recent(int limit) {
        return recent.stream().limit(limit).toList();
    }
}
