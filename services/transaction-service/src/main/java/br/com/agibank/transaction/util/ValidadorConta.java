package br.com.agibank.transaction.util;

import org.springframework.stereotype.Component;

@Component
public class ValidadorConta {

    public boolean isNumeroContaValido(String numeroConta) {
        if (numeroConta == null || numeroConta.trim().isEmpty()) {
            return false;
        }

        String conta = numeroConta.trim();
        return conta.matches("\\d{8}");
    }

    public String formatarNumeroConta(String numeroConta) {
        if (numeroConta == null) {
            return null;
        }

        String conta = numeroConta.replaceAll("\\D", "");

        if (conta.length() == 8) {
            return conta;
        }

        throw new IllegalArgumentException("Número da conta deve ter 8 dígitos");
    }

    public String mascararNumeroConta(String numeroConta) {
        if (numeroConta == null || numeroConta.length() != 8) {
            return numeroConta;
        }

        return "****" + numeroConta.substring(4);
    }
}
