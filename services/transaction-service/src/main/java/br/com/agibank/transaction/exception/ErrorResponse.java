package br.com.agibank.transaction.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resposta de erro padrão")
public record ErrorResponse(
        @Schema(description = "Código do erro", example = "VALIDATION_ERROR")
        String codigo,

        @Schema(description = "Mensagem do erro", example = "Dados inválidos")
        String mensagem,

        @Schema(description = "Detalhes do erro")
        String detalhes,

        @Schema(description = "Data e hora do erro")
        LocalDateTime dataHora
) {
}
