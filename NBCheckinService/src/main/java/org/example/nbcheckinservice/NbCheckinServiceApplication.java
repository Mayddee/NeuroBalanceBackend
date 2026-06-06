package org.example.nbcheckinservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableKafka
@EnableAsync
public class NbCheckinServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NbCheckinServiceApplication.class, args);
    }

}
