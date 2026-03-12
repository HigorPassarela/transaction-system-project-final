package br.com.agibank.extract.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

@Configuration
public class DatabaseConfig {

    // Transaction Manager para JPA/Camunda
    @Bean
    @Primary
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // Transaction Manager para MongoDB (se necessário)
    @Bean("mongoTransactionManager")
    public MongoTransactionManager mongoTransactionManager(MongoTemplate mongoTemplate) {
        return new MongoTransactionManager(mongoTemplate.getMongoDatabaseFactory());
    }
}
