package br.com.agibank.transaction.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Resposta da consulta de saldo")
public record SaldoResponse(
        @Schema(description = "Número da conta", example = "12345678")
        String numeroConta,

        @Schema(description = "Saldo atual da conta", example = "1500.75")
        BigDecimal saldo,

        @Schema(description = "Limite de crédito disponível", example = "3000.00")
        BigDecimal limiteCredito,

        @Schema(description = "Data da última atualização")
        LocalDateTime ultimaAtualizacao
) {
}
