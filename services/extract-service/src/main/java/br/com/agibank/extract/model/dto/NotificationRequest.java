package br.com.agibank.extract.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NotificationRequest(
        String idTransacao,
        String numeroConta,
        BigDecimal valor,
        String tipo,
        String status,
        String descricao,
        LocalDateTime dataHora,
        BigDecimal saldoAtual
) {

    public static NotificationRequest fromTransaction(TransactionDTO transaction) {
        return new NotificationRequest(
                transaction.idTransacao(),
                transaction.numeroConta(),
                transaction.valor(),
                transaction.tipo(),
                transaction.status(),
                transaction.descricao(),
                transaction.dataHora(),
                transaction.saldoAtual()
        );
    }
}
