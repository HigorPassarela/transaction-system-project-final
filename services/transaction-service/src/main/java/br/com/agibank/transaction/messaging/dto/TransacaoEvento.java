package br.com.agibank.transaction.messaging.dto;

import br.com.agibank.transaction.model.enums.StatusTransacao;
import br.com.agibank.transaction.model.enums.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoEvento(
        String idTransacao,
        String numeroConta,
        BigDecimal valor,
        TipoTransacao tipo,
        StatusTransacao status,
        String descricao,
        LocalDateTime dataHora,
        BigDecimal saldoAtual
) {

    public TransacaoEvento {
        if (dataHora == null) {
            dataHora = LocalDateTime.now();
        }
    }

    public static TransacaoEvento transacaoAprovada(
            String idTransacao,
            String numeroConta,
            BigDecimal valor,
            TipoTransacao tipo,
            String descricao,
            BigDecimal saldoAtual) {

        return new TransacaoEvento(
                idTransacao,
                numeroConta,
                valor,
                tipo,
                StatusTransacao.APROVADA,
                descricao,
                LocalDateTime.now(),
                saldoAtual
        );
    }
}
