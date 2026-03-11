package br.com.agibank.notifications.messaging.consumer;

import br.com.agibank.notifications.model.dto.TransacaoEvento;
import br.com.agibank.notifications.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class TransacaoConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransacaoConsumer.class);

    private final NotificationService notificationService;

    public TransacaoConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "transacoes-aprovadas", groupId = "notifications-service-group")
    public void processarNotificacao(
            @Payload TransacaoEvento evento,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("📢 === EVENTO DE NOTIFICAÇÃO RECEBIDO ===");
        logger.info("📍 Tópico: {}, Partição: {}, Offset: {}", topic, partition, offset);
        logger.info("💰 Transação: ID={}, Conta={}, Tipo={}, Valor={}",
                evento.idTransacao(), evento.numeroConta(), evento.tipo(), evento.valor());

        try {
            notificationService.enviarNotificacao(evento);
            acknowledgment.acknowledge();
            logger.info("✅ Notificação enviada com sucesso para conta {}", evento.numeroConta());
        } catch (Exception e) {
            logger.error("❌ Erro ao processar notificação para transação {}: {}",
                    evento.idTransacao(), e.getMessage(), e);
            throw e;
        }
    }
}
