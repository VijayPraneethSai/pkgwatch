package dev.vijay.pkgwatch.messaging;

import dev.vijay.pkgwatch.ingestion.IngestionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UsageListener {

    private final IngestionService ingestion;

    public UsageListener(IngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @KafkaListener(
            id = "pkgwatch-downloads",
            topics = "${pkgwatch.topics.downloads}",
            concurrency = "${pkgwatch.topics.partitions:3}")
    public void onDownloads(ConsumerRecord<String, String> record) {
        ingestion.process(record.value());
    }
}
