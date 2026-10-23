package dev.vijay.pkgwatch.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic downloadsTopic(PkgwatchProperties props) {
        return TopicBuilder.name(props.topics().downloads())
                .partitions(props.topics().partitions())
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic deadLetterTopic(PkgwatchProperties props) {
        return TopicBuilder.name(props.topics().dlq())
                .partitions(1)
                .replicas(1)
                .build();
    }
}
