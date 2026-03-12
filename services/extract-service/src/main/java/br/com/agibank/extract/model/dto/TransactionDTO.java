package br.com.agibank.extract.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Dados de uma transação")
public record TransactionDTO(
        @JsonProperty("idTransacao")
        @Schema(description = "ID único da transação", example = "TXN-12345678")
        String idTransacao,

        @JsonProperty("numeroConta")
        @Schema(description = "Número da conta", example = "12345678")
        String numeroConta,

        @JsonProperty("valor")
        @Schema(description = "Valor da transação", example = "150.75")
        BigDecimal valor,

        @JsonProperty("tipo")
        @Schema(description = "Tipo da transação", example = "DEBITO")
        String tipo,

        @JsonProperty("status")
        @Schema(description = "Status da transação", example = "APROVADA")
        String status,

        @JsonProperty("descricao")
        @Schema(description = "Descrição da transação", example = "Compra no supermercado")
        String descricao,

        @JsonProperty("dataHora")
        @Schema(description = "Data e hora da transação")
        LocalDateTime dataHora,

        @JsonProperty("saldoAtual")
        @Schema(description = "Saldo atual após a transação", example = "849.25")
        BigDecimal saldoAtual
) {

    public TransactionDTO {
        if (dataHora == null) {
            dataHora = LocalDateTime.now();
        }
    }
}
