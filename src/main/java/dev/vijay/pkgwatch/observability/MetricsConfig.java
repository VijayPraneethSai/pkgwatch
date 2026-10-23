package dev.vijay.pkgwatch.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.MicrometerConsumerListener;

@Configuration
public class MetricsConfig {

    @Bean
    @SuppressWarnings("unchecked")
    DefaultKafkaConsumerFactoryCustomizer consumerMetricsCustomizer(MeterRegistry registry) {
        return factory -> ((DefaultKafkaConsumerFactory<Object, Object>) factory)
                .addListener(new MicrometerConsumerListener<>(registry));
    }
}
