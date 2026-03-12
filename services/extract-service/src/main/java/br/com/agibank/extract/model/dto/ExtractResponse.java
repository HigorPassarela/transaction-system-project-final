package br.com.agibank.extract.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Resposta com dados do extrato")
public record ExtractResponse(
        @Schema(description = "Número da conta", example = "12345678")
        String numeroConta,

        @Schema(description = "Data inicial do período")
        LocalDate dataInicial,

        @Schema(description = "Data final do período")
        LocalDate dataFinal,

        @Schema(description = "Saldo no início do período", example = "1000.00")
        BigDecimal saldoInicial,

        @Schema(description = "Saldo no final do período", example = "1500.00")
        BigDecimal saldoFinal,

        @Schema(description = "Lista de transações do período")
        List<TransactionDTO> transacoes,

        @Schema(description = "Total de transações encontradas", example = "15")
        int totalTransacoes,

        @Schema(description = "Data e hora de geração do extrato")
        LocalDateTime dataGeracao
) {

    public ExtractResponse {
        if (dataGeracao == null) {
            dataGeracao = LocalDateTime.now();
        }
    }
}
