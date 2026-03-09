package br.com.agibank.transaction.messaging.producer;

import br.com.agibank.transaction.messaging.dto.TransacaoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.CompletableFuture;

@Component
public class TransacaoProducer {

    private static final Logger logger = LoggerFactory.getLogger(TransacaoProducer.class);
    private static final String TOPICO_TRANSACOES_APROVADAS = "transacoes-aprovadas";

    private final KafkaTemplate<String, TransacaoEvento> kafkaTemplate;

    public TransacaoProducer(KafkaTemplate<String, TransacaoEvento> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void enviarTransacaoAprovada(TransacaoEvento evento) {
        logger.info("Enviando transação aprovada para Kafka: {}", evento.idTransacao());

        CompletableFuture<SendResult<String, TransacaoEvento>> future =
                kafkaTemplate.send(TOPICO_TRANSACOES_APROVADAS, evento.numeroConta(), evento);

        future.whenComplete((result, exception) -> {
            if (exception == null) {
                logger.info("Transação {} enviada com sucesso para o tópico {} na partição {}",
                        evento.idTransacao(),
                        TOPICO_TRANSACOES_APROVADAS,
                        result.getRecordMetadata().partition());
            } else {
                logger.error("Erro ao enviar transação {} para Kafka: {}",
                        evento.idTransacao(), exception.getMessage());
            }
        });
    }
}
