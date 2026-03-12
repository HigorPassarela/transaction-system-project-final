package br.com.agibank.extract.service;

import br.com.agibank.extract.model.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.Duration;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Value("${services.notifications-service.url}")
    private String notificationsServiceUrl;

    private final WebClient webClient;

    public NotificationService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    public boolean enviarNotificacao(NotificationRequest request) {
        logger.info("Enviando notificação para conta {}: transação {}",
                request.numeroConta(), request.idTransacao());

        try {
            String response = webClient.post()
                    .uri(notificationsServiceUrl + "/api/v1/notifications/send")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            logger.info("✅ Notificação enviada com sucesso: {}", response);
            return true;

        } catch (WebClientException e) {
            logger.error("❌ Erro de comunicação ao enviar notificação: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("❌ Erro inesperado ao enviar notificação: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean testarConexao() {
        try {
            String response = webClient.get()
                    .uri(notificationsServiceUrl + "/api/v1/notifications/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            logger.info("Teste de conexão com Notifications Service: OK - {}", response);
            return true;

        } catch (Exception e) {
            logger.error("Teste de conexão com Notifications Service: FALHOU - {}", e.getMessage());
            return false;
        }
    }
}
