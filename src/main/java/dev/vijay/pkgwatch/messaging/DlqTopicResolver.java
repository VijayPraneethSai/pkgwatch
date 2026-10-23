package dev.vijay.pkgwatch.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

public final class DlqTopicResolver {

    private DlqTopicResolver() {
    }

    public static TopicPartition resolve(ConsumerRecord<?, ?> record, Exception exception) {
        return new TopicPartition(record.topic() + ".dlq", -1);
    }
}
