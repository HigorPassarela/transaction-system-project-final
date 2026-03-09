package br.com.agibank.transaction.exception;

public class TransacaoException extends RuntimeException{

    private final String detalhes;

    public TransacaoException(String mensagem) {
        super(mensagem);
        this.detalhes = null;
    }

    public TransacaoException(String mensagem, String detalhes) {
        super(mensagem);
        this.detalhes = detalhes;
    }

    public TransacaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.detalhes = causa.getMessage();
    }

    public String getDetalhes() {
        return detalhes;
    }
}
