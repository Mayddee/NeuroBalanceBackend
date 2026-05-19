package org.example.nbcheckinservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka конфигурация для NBCheckinService.
 *
 * Топики:
 *   checkin.created — публикуется при успешном создании DailyCheckIn.
 *                     HealthMetricsKafkaConsumer подписывается и вычисляет метрики асинхронно.
 *
 * Producer — кастомный (этот бин), переопределяет spring.kafka.producer.* из application.properties.
 * Consumer — авто-конфигурация Spring Boot, читает spring.kafka.consumer.* из application.properties.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.checkin-created:checkin.created}")
    private String checkinCreatedTopic;

    @Bean
    public NewTopic checkinCreatedTopic() {
        return TopicBuilder.name(checkinCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Правильные Kafka-сериализаторы (НЕ Jackson-классы!)
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.springframework.kafka.support.serializer.JsonSerializer");
        // Добавляем __TypeId__ header — consumer использует его для десериализации в нужный класс
        configProps.put("spring.json.add.type.headers", true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
