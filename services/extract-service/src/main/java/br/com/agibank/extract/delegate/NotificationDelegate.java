package br.com.agibank.extract.delegate;

import br.com.agibank.extract.model.dto.NotificationRequest;
import br.com.agibank.extract.service.NotificationService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component("notificationDelegate")
public class NotificationDelegate implements JavaDelegate{

    private static final Logger logger = LoggerFactory.getLogger(NotificationDelegate.class);

    private final NotificationService notificationService;

    public NotificationDelegate(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("=== EXECUTANDO DELEGATE DE NOTIFICAÇÃO ===");
        logger.info("Process Instance ID: {}", execution.getProcessInstanceId());
        logger.info("Business Key: {}", execution.getProcessBusinessKey());

        try {
            // Recuperar dados das variáveis do processo
            String idTransacao = (String) execution.getVariable("idTransacao");
            String numeroConta = (String) execution.getVariable("numeroConta");

            // Tratar BigDecimal que pode vir como Double do processo
            BigDecimal valor = convertToBigDecimal(execution.getVariable("valor"));
            BigDecimal saldoAtual = convertToBigDecimal(execution.getVariable("saldoAtual"));

            String tipo = (String) execution.getVariable("tipo");
            String status = (String) execution.getVariable("status");
            String descricao = (String) execution.getVariable("descricao");

            // Tratar LocalDateTime que pode vir serializado
            LocalDateTime dataHora = convertToLocalDateTime(execution.getVariable("dataHora"));

            logger.info("Processando notificação - Transação: {}, Conta: {}, Valor: {}",
                    idTransacao, numeroConta, valor);

            // Criar request de notificação
            NotificationRequest request = new NotificationRequest(
                    idTransacao, numeroConta, valor, tipo, status, descricao, dataHora, saldoAtual
            );

            // Enviar notificação
            boolean sucesso = notificationService.enviarNotificacao(request);

            // Definir variáveis de resultado
            execution.setVariable("notificationSent", sucesso);
            execution.setVariable("notificationTimestamp", LocalDateTime.now().toString());

            if (sucesso) {
                logger.info("✅ Notificação enviada com sucesso para transação {}", idTransacao);
            } else {
                logger.warn("⚠️ Falha ao enviar notificação para transação {}", idTransacao);
                execution.setVariable("notificationError", "Serviço de notificações indisponível");
            }

        } catch (Exception e) {
            logger.error("❌ Erro no delegate de notificação: {}", e.getMessage(), e);

            // Definir variáveis de erro
            execution.setVariable("notificationSent", false);
            execution.setVariable("notificationError", e.getMessage());
            execution.setVariable("notificationTimestamp", LocalDateTime.now().toString());

            // Não relançar exceção para não falhar o processo
        }
    }

    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Double) return BigDecimal.valueOf((Double) value);
        if (value instanceof String) return new BigDecimal((String) value);
        return BigDecimal.ZERO;
    }

    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null) return LocalDateTime.now();
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof String) return LocalDateTime.parse((String) value);
        return LocalDateTime.now();
    }
}
