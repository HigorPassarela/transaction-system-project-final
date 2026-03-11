package br.com.agibank.extract;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class ExtractApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExtractApplication.class, args);
    }
}
