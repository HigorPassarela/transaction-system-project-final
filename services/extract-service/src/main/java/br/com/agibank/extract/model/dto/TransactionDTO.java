package br.com.agibank.extract.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionDTO(
        @JsonProperty("idTransacao")
        String idTransacao,

        @JsonProperty("numeroConta")
        String numeroConta,

        @JsonProperty("valor")
        BigDecimal valor,

        @JsonProperty("tipo")
        String tipo,

        @JsonProperty("status")
        String status,

        @JsonProperty("descricao")
        String descricao,

        @JsonProperty("dataHora")
        LocalDateTime dataHora,

        @JsonProperty("saldoAtual")
        BigDecimal saldoAtual
) {
}
