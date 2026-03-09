package br.com.agibank.transaction.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RedisHash("saldo_contas")
public class SaldoConta {

    @Id
    private String numeroConta;
    private BigDecimal saldo;
    private BigDecimal limiteCredito;
    private LocalDateTime ultimaAtualizacao;

    public SaldoConta() {
        this.ultimaAtualizacao = LocalDateTime.now();
    }

    public SaldoConta(String numeroConta, BigDecimal saldo, BigDecimal limiteCredito) {
        this();
        this.numeroConta = numeroConta;
        this.saldo = saldo;
        this.limiteCredito = limiteCredito;
    }

    public boolean podeDebitar(BigDecimal valor) {
        return saldo.compareTo(valor) >= 0;
    }

    public boolean podeCreditar(BigDecimal valor) {
        return limiteCredito.compareTo(valor) >= 0;
    }

    public void debitar(BigDecimal valor) {
        this.saldo = this.saldo.subtract(valor);
        this.ultimaAtualizacao = LocalDateTime.now();
    }

    public void creditar(BigDecimal valor) {
        this.saldo = this.saldo.add(valor);
        this.limiteCredito = this.limiteCredito.subtract(valor);
        this.ultimaAtualizacao = LocalDateTime.now();
    }

    public String getNumeroConta() {
        return numeroConta;
    }
    public void setNumeroConta(String numeroConta) {
        this.numeroConta = numeroConta;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }
    public void setLimiteCredito(BigDecimal limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }
    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }
}
