package org.ecommerce.paymentservice.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaMessageConfig {

    @Bean
    NewTopic paymentEventsTopic(@Value("${application.kafka.topics.payment-events:payment-events}") String topicName) {
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic paymentEventsDlqTopic(@Value("${application.kafka.topics.payment-events-dlq:payment-events-dlq}") String topicName) {
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }
}

