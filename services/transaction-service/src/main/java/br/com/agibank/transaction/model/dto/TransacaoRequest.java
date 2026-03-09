package br.com.agibank.transaction.model.dto;

import br.com.agibank.transaction.model.enums.TipoTransacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados da solicitação de transação")
public record TransacaoRequest(
        @NotBlank(message = "Número da conta é obrigatório")
        @Pattern(regexp = "\\d{8}", message = "Conta deve ter 8 dígitos")
        @Schema(description = "Número da conta", example = "12345678")
        String numeroConta,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
        @DecimalMax(value = "50000.00", message = "Valor máximo é R$ 50.000,00")
        @Schema(description = "Valor da transação", example = "1000.50")
        BigDecimal valor,

        @NotNull(message = "Tipo de transação é obrigatório")
        @Schema(description = "Tipo da transação")
        TipoTransacao tipo,

        @Size(max = 100, message = "Descrição deve ter no máximo 100 caracteres")
        @Schema(description = "Descrição da transação", example = "Compra no supermercado")
        String descricao
) {

    public TransacaoRequest {
        if (numeroConta != null) {
            numeroConta = numeroConta.trim();
        }
        if (descricao != null) {
            descricao = descricao.trim();
        }
    }

    public boolean isDebito() {
        return TipoTransacao.DEBITO.equals(tipo);
    }

    public boolean isCredito() {
        return TipoTransacao.CREDITO.equals(tipo);
    }
}
