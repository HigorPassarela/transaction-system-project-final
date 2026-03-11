package br.com.agibank.extract.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.database:agibank_extracts}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }
}
