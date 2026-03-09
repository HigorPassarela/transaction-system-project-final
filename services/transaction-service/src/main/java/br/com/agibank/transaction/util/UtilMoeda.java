package br.com.agibank.transaction.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class UtilMoeda {

    private static final Locale LOCALE_BRASIL = new Locale("pt", "BR");
    private static final NumberFormat FORMATO_MOEDA = NumberFormat.getCurrencyInstance(LOCALE_BRASIL);

    public static String formatarMoeda(BigDecimal valor) {
        if (valor == null) {
            return "R$ 0,00";
        }
        return FORMATO_MOEDA.format(valor);
    }

    public static BigDecimal arredondarMoeda(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isValorPositivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isValorValido(BigDecimal valor, BigDecimal minimo, BigDecimal maximo) {
        if (valor == null) {
            return false;
        }

        return valor.compareTo(minimo) >= 0 && valor.compareTo(maximo) <= 0;
    }

    public static BigDecimal centavosParaReais(Long centavos) {
        if (centavos == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(centavos).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public static Long reaisParaCentavos(BigDecimal reais) {
        if (reais == null) {
            return 0L;
        }

        return reais.multiply(new BigDecimal("100")).longValue();
    }
}
