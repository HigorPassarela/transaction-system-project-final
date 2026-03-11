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

        logger.info("🎉 === MENSAGEM RECEBIDA DO KAFKA ===");
        logger.info("📍 Tópico: {}, Partição: {}, Offset: {}", topic, partition, offset);
        logger.info("💰 Transação: ID={}, Conta={}, Valor={}, Tipo={}",
                transacao.idTransacao(), transacao.numeroConta(),
                transacao.valor(), transacao.tipo());

        try {
            extractService.registrarTransacao(transacao);
            acknowledgment.acknowledge();
            logger.info("✅ Transação {} processada e confirmada no MongoDB!", transacao.idTransacao());
        } catch (Exception e) {
            logger.error("❌ Erro ao processar transação {}: {}", transacao.idTransacao(), e.getMessage(), e);
            throw e;
        }
    }
}
