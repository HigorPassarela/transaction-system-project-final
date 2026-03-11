package br.com.agibank.extract.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

@Schema(description = "Dados para solicitação de extrato")
public record ExtractRequest(
        @NotBlank(message = "Número da conta é obrigatório")
        @Pattern(regexp = "\\d{8}", message = "Conta deve ter 8 dígitos")
        @Schema(description = "Número da conta", example = "12345678")
        String numeroConta,

        @NotNull(message = "Data inicial é obrigatória")
        @Schema(description = "Data inicial do período", example = "2024-01-01")
        LocalDate dataInicial,

        @NotNull(message = "Data final é obrigatória")
        @Schema(description = "Data final do período", example = "2024-01-31")
        LocalDate dataFinal
) {
    public ExtractRequest {
        if (numeroConta != null) {
            numeroConta = numeroConta.trim();
        }

        if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException("Data inicial não pode ser posterior à data final");
        }
    }
}
