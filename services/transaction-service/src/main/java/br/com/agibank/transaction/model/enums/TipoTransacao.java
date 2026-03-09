package br.com.agibank.transaction.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de transação")
public enum TipoTransacao {
    @Schema(description = "Transação de débito - saída de dinheiro")
    DEBITO,
    @Schema(description = "Transação de crédito - entrada de dinheiro")
    CREDITO
}
