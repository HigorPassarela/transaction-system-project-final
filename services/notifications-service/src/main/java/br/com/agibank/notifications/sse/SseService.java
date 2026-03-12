package br.com.agibank.notifications.sse;

import br.com.agibank.notifications.model.dto.NotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private static final Logger logger = LoggerFactory.getLogger(SseService.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutos

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> connections = new ConcurrentHashMap<>();

    public SseEmitter conectar(String numeroConta) {
        logger.info("Nova conexão SSE para conta: {}", numeroConta);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        connections.computeIfAbsent(numeroConta, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> {
            logger.info("Conexão SSE finalizada para conta: {}", numeroConta);
            removerEmitter(numeroConta, emitter);
        });

        emitter.onTimeout(() -> {
            logger.info("Timeout da conexão SSE para conta: {}", numeroConta);
            removerEmitter(numeroConta, emitter);
        });

        emitter.onError((ex) -> {
            logger.error("Erro na conexão SSE para conta {}: {}", numeroConta, ex.getMessage());
            removerEmitter(numeroConta, emitter);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Conectado ao serviço de notificações"));
        } catch (IOException e) {
            logger.error("Erro ao enviar mensagem de conexão: {}", e.getMessage());
            removerEmitter(numeroConta, emitter);
        }

        logger.info("Conexões ativas para conta {}: {}", numeroConta,
                connections.getOrDefault(numeroConta, new CopyOnWriteArrayList<>()).size());

        return emitter;
    }

    public void enviarNotificacao(String numeroConta, NotificationDTO notificacao) {
        CopyOnWriteArrayList<SseEmitter> emitters = connections.get(numeroConta);

        if (emitters == null || emitters.isEmpty()) {
            logger.warn("Nenhuma conexão ativa para conta {}", numeroConta);
            return;
        }

        logger.info("Enviando notificação para {} conexões da conta {}",
                emitters.size(), numeroConta);

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notificacao));

                logger.debug("Notificação enviada com sucesso para uma conexão da conta {}", numeroConta);

            } catch (IOException e) {
                logger.error("Erro ao enviar notificação para conta {}: {}", numeroConta, e.getMessage());
                removerEmitter(numeroConta, emitter);
            }
        });
    }

    private void removerEmitter(String numeroConta, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = connections.get(numeroConta);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                connections.remove(numeroConta);
                logger.info("Removida lista vazia de conexões para conta {}", numeroConta);
            }
        }
        emitter.complete();
    }

    public void enviarNotificacaoGenerica(String numeroConta, Map<String, Object> notificacao) {
        CopyOnWriteArrayList<SseEmitter> emitters = connections.get(numeroConta);

        if (emitters == null || emitters.isEmpty()) {
            logger.warn("Nenhuma conexão ativa para conta {}", numeroConta);
            return;
        }

        logger.info("Enviando notificação genérica para {} conexões da conta {}",
                emitters.size(), numeroConta);

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notificacao));

                logger.debug("Notificação enviada com sucesso para uma conexão da conta {}", numeroConta);

            } catch (IOException e) {
                logger.error("Erro ao enviar notificação para conta {}: {}", numeroConta, e.getMessage());
                removerEmitter(numeroConta, emitter);
            }
        });
    }

    public int getConexoesAtivas(String numeroConta) {
        return connections.getOrDefault(numeroConta, new CopyOnWriteArrayList<>()).size();
    }

    public int getTotalConexoes() {
        return connections.values().stream()
                .mapToInt(CopyOnWriteArrayList::size)
                .sum();
    }
}
