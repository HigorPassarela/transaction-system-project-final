package br.com.agibank.notifications.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NotificationDTO(
        String id,
        String numeroConta,
        String titulo,
        String mensagem,
        String tipo,
        BigDecimal valor,
        LocalDateTime dataHora,
        boolean lida
) {

    public static NotificationDTO fromTransacao(TransacaoEvento evento) {
        String titulo = "CREDITO".equals(evento.tipo()) ?
                "💳 Crédito Processado" : "💰 Débito Processado";

        String mensagem = String.format(
                "Transação de %s no valor de R$ %.2f foi processada com sucesso. Saldo atual: R$ %.2f",
                evento.tipo().toLowerCase(),
                evento.valor(),
                evento.saldoAtual()
        );

        return new NotificationDTO(
                evento.idTransacao(),
                evento.numeroConta(),
                titulo,
                mensagem,
                evento.tipo(),
                evento.valor(),
                evento.dataHora(),
                false
        );
    }
}
