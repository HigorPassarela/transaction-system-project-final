package br.com.agibank.transaction.model.dto;

import br.com.agibank.transaction.model.enums.StatusTransacao;
import br.com.agibank.transaction.model.enums.TipoTransacao;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Resposta da transação processada")
public record TransacaoResponse(
        @Schema(description = "ID único da transação", example = "TXN-123456789")
        String idTransacao,

        @Schema(description = "Número da conta", example = "12345678")
        String numeroConta,

        @Schema(description = "Valor da transação", example = "1000.50")
        BigDecimal valor,

        @Schema(description = "Tipo da transação")
        TipoTransacao tipo,

        @Schema(description = "Status da transação")
        StatusTransacao status,

        @Schema(description = "Mensagem explicativa", example = "Transação processada com sucesso")
        String mensagem,

        @Schema(description = "Data e hora da transação")
        LocalDateTime dataHora,

        @Schema(description = "Saldo atual da conta", example = "2500.75")
        BigDecimal saldoAtual
) {

    public TransacaoResponse {
        if (dataHora == null) {
            dataHora = LocalDateTime.now();
        }
    }

    public static TransacaoResponse aprovada(
            String idTransacao,
            String numeroConta,
            BigDecimal valor,
            TipoTransacao tipo,
            BigDecimal saldoAtual) {

        return new TransacaoResponse(
                idTransacao,
                numeroConta,
                valor,
                tipo,
                StatusTransacao.APROVADA,
                "Transação aprovada e processada com sucesso",
                LocalDateTime.now(),
                saldoAtual
        );
    }

    public static TransacaoResponse rejeitada(
            String idTransacao,
            String numeroConta,
            BigDecimal valor,
            TipoTransacao tipo,
            String motivo) {

        return new TransacaoResponse(
                idTransacao,
                numeroConta,
                valor,
                tipo,
                StatusTransacao.REJEITADA,
                "Transação rejeitada: " + motivo,
                LocalDateTime.now(),
                null
        );
    }

    public boolean isAprovada() {
        return StatusTransacao.APROVADA.equals(status);
    }

    public boolean isRejeitada() {
        return StatusTransacao.REJEITADA.equals(status);
    }
}
