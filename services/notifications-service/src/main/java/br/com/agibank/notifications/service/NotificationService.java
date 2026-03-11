package br.com.agibank.notifications.service;

import br.com.agibank.notifications.model.dto.NotificationDTO;
import br.com.agibank.notifications.model.dto.TransacaoEvento;
import br.com.agibank.notifications.sse.SseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final SseService sseService;

    public NotificationService(SseService sseService) {
        this.sseService = sseService;
    }

    public void enviarNotificacao(TransacaoEvento evento) {
        logger.info("Processando notificação para transação {}", evento.idTransacao());

        try {
            NotificationDTO notificacao = NotificationDTO.fromTransacao(evento);

            logger.info("Enviando notificação SSE para conta {}: {}",
                    evento.numeroConta(), notificacao.titulo());

            sseService.enviarNotificacao(evento.numeroConta(), notificacao);

            logger.info("Notificação enviada com sucesso para conta {}", evento.numeroConta());

        } catch (Exception e) {
            logger.error("Erro ao enviar notificação para transação {}: {}",
                    evento.idTransacao(), e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar notificação", e);
        }
    }
}
