package br.com.agibank.extract.config;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class CamundaConfig {

    private static final Logger logger = LoggerFactory.getLogger(CamundaConfig.class);

    @Bean
    public CommandLineRunner deployProcesses(ProcessEngine processEngine) {
        return args -> {
            RepositoryService repositoryService = processEngine.getRepositoryService();

            try {
                // Deploy do processo BPMN
                repositoryService.createDeployment()
                        .addClasspathResource("processes/transaction-notification-process.bpmn")
                        .name("Transaction Notification Process")
                        .deploy();

                logger.info("✅ Processo BPMN deployado com sucesso!");

            } catch (Exception e) {
                logger.error("❌ Erro ao fazer deploy do processo BPMN: {}", e.getMessage(), e);
            }
        };
    }
}
