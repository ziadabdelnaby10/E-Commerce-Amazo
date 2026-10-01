package org.ecommerce.inventoryservice.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaMessageConfig {

    @Bean
    NewTopic inventoryEventsTopic(@Value("${application.kafka.topics.inventory-events:inventory-events}") String topicName) {
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic inventoryEventsDlqTopic(@Value("${application.kafka.topics.inventory-events-dlq:inventory-events-dlq}") String topicName) {
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }

    @Bean
    ProducerFactory<String, String> inventoryProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(producerProps);
    }

    @Bean("inventoryKafkaTemplate")
    KafkaTemplate<String, String> inventoryKafkaTemplate(ProducerFactory<String, String> inventoryProducerFactory) {
        return new KafkaTemplate<>(inventoryProducerFactory);
    }

    @Bean
    ConsumerFactory<String, String> orderEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${application.kafka.consumers.order-group-id:inventory-service-orders}") String groupId) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
    }

    @Bean("orderEventKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> orderEventKafkaListenerContainerFactory(
            ConsumerFactory<String, String> orderEventConsumerFactory,
            KafkaTemplate<String, String> inventoryKafkaTemplate,
            @Value("${application.kafka.topics.inventory-events-dlq:inventory-events-dlq}") String inventoryDlqTopic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                inventoryKafkaTemplate,
                (record, ex) -> new TopicPartition(inventoryDlqTopic, record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderEventConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    ConsumerFactory<String, String> paymentEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${application.kafka.consumers.payment-group-id:inventory-service-payments}") String groupId) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
    }

    @Bean("paymentEventKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> paymentEventKafkaListenerContainerFactory(
            ConsumerFactory<String, String> paymentEventConsumerFactory,
            KafkaTemplate<String, String> inventoryKafkaTemplate,
            @Value("${application.kafka.topics.inventory-events-dlq:inventory-events-dlq}") String inventoryDlqTopic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                inventoryKafkaTemplate,
                (record, ex) -> new TopicPartition(inventoryDlqTopic, record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentEventConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}

