package br.com.agibank.transaction.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status da transação")
public enum StatusTransacao {
    @Schema(description = "Transação aprovada")
    APROVADA,
    @Schema(description = "Transação rejeitada")
    REJEITADA,
    @Schema(description = "Transação processada com sucesso")
    PROCESSADA
}
