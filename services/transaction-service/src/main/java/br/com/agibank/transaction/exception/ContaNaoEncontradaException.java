package br.com.agibank.transaction.exception;

public class ContaNaoEncontradaException extends RuntimeException{

    private final String numeroConta;

    public ContaNaoEncontradaException(String numeroConta) {
        super("Conta não encontrada: " + numeroConta);
        this.numeroConta = numeroConta;
    }

    public String getNumeroConta() {
        return numeroConta;
    }
}
