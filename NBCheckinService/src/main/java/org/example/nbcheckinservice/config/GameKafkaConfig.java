package org.example.nbcheckinservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topics for game progression and character events.
 * Does NOT touch existing KafkaConfig — only adds new topics.
 */
@Configuration
public class GameKafkaConfig {

    @Value("${kafka.topics.character-leveled-up:character.leveled-up}")
    private String characterLeveledUpTopic;

    @Bean
    public NewTopic characterLeveledUpTopic() {
        return TopicBuilder.name(characterLeveledUpTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}