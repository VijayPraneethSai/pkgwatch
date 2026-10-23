package dev.vijay.pkgwatch.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DlqTopicResolverTest {

    @Test
    void routesToTheDeadLetterTopicWithAnyPartition() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("package.downloads", 2, 15L, "lodash", "{}");

        TopicPartition target = DlqTopicResolver.resolve(record, new IllegalArgumentException("bad"));

        assertEquals("package.downloads.dlq", target.topic());
        assertEquals(-1, target.partition());
    }
}
