package org.example.ainote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class AiNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiNoteApplication.class, args);
    }

}
