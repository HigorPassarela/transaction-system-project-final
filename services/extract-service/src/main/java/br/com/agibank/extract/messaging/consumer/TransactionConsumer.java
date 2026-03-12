package br.com.agibank.extract.messaging.consumer;

import br.com.agibank.extract.model.dto.TransactionDTO;
import br.com.agibank.extract.service.ExtractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);

    private final ExtractService extractService;

    public TransactionConsumer(ExtractService extractService) {
        this.extractService = extractService;
    }

    @KafkaListener(topics = "transacoes-aprovadas", groupId = "extract-service-group")
    public void processarTransacaoAprovada(
            @Payload TransactionDTO transacao,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("🎉 === TRANSAÇÃO RECEBIDA DO KAFKA ===");
        logger.info("📍 Tópico: {}, Partição: {}, Offset: {}", topic, partition, offset);
        logger.info("💰 Transação: ID={}, Conta={}, Valor={}, Tipo={}",
                transacao.idTransacao(), transacao.numeroConta(),
                transacao.valor(), transacao.tipo());

        try {
            // Validação básica
            if (transacao.idTransacao() == null || transacao.numeroConta() == null) {
                logger.error("❌ Transação inválida recebida: dados obrigatórios ausentes");
                acknowledgment.acknowledge(); // Acknowledge para não reprocessar
                return;
            }

            // Verificar se já foi processada
            if (extractService.transacaoJaProcessada(transacao.idTransacao())) {
                logger.warn("⚠️ Transação {} já foi processada anteriormente", transacao.idTransacao());
                acknowledgment.acknowledge();
                return;
            }

            // Processar transação (registra no MongoDB + inicia workflow Camunda)
            extractService.registrarTransacao(transacao);

            // Confirmar processamento
            acknowledgment.acknowledge();

            logger.info("✅ Transação {} processada com sucesso!", transacao.idTransacao());

        } catch (Exception e) {
            logger.error("❌ Erro ao processar transação {}: {}", transacao.idTransacao(), e.getMessage(), e);

            // Em produção, você pode implementar:
            // 1. Retry com backoff
            // 2. Dead letter queue
            // 3. Alertas para equipe de suporte

            // Por ora, vamos acknowledge para não travar a fila
            acknowledgment.acknowledge();
        }
    }
}
