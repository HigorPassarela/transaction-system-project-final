package br.com.agibank.extract.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "transactions")
@CompoundIndexes({
        @CompoundIndex(name = "conta_data_idx", def = "{'numeroConta': 1, 'dataHora': -1}"),
        @CompoundIndex(name = "conta_tipo_idx", def = "{'numeroConta': 1, 'tipo': 1}"),
        @CompoundIndex(name = "status_data_idx", def = "{'status': 1, 'dataHora': -1}")
})
public class TransactionRecord {

    @Id
    private String id;

    @Field("transactionId")
    @Indexed(unique = true)
    private String transactionId;

    @Field("numeroConta")
    @Indexed
    private String numeroConta;

    @Field("valor")
    private BigDecimal valor;

    @Field("tipo")
    @Indexed
    private String tipo;

    @Field("descricao")
    private String descricao;

    @Field("status")
    @Indexed
    private String status;

    @Field("dataHora")
    @Indexed
    private LocalDateTime dataHora;

    @Field("saldoAtual")
    private BigDecimal saldoAtual;

    @Field("createdAt")
    private LocalDateTime createdAt;

    // === CAMPOS PARA CAMUNDA ===
    @Field("processedByCamunda")
    private Boolean processedByCamunda = false;

    @Field("workflowInstanceId")
    private String workflowInstanceId;

    // Construtores
    public TransactionRecord() {
        this.createdAt = LocalDateTime.now();
        this.processedByCamunda = false;
    }

    public TransactionRecord(String transactionId, String numeroConta, BigDecimal valor,
                             String tipo, String descricao, String status,
                             LocalDateTime dataHora, BigDecimal saldoAtual) {
        this();
        this.transactionId = transactionId;
        this.numeroConta = numeroConta;
        this.valor = valor;
        this.tipo = tipo;
        this.descricao = descricao;
        this.status = status;
        this.dataHora = dataHora;
        this.saldoAtual = saldoAtual;
    }

    // Getters e Setters COMPLETOS
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public void setNumeroConta(String numeroConta) {
        this.numeroConta = numeroConta;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public BigDecimal getSaldoAtual() {
        return saldoAtual;
    }

    public void setSaldoAtual(BigDecimal saldoAtual) {
        this.saldoAtual = saldoAtual;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // === GETTERS E SETTERS PARA CAMUNDA ===
    public Boolean getProcessedByCamunda() {
        return processedByCamunda;
    }

    public void setProcessedByCamunda(Boolean processedByCamunda) {
        this.processedByCamunda = processedByCamunda;
    }

    // Método de conveniência
    public boolean isProcessedByCamunda() {
        return processedByCamunda != null && processedByCamunda;
    }

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    @Override
    public String toString() {
        return String.format(
                "TransactionRecord{id='%s', transactionId='%s', numeroConta='%s', valor=%s, tipo='%s', processedByCamunda=%s, workflowInstanceId='%s'}",
                id, transactionId, numeroConta, valor, tipo, processedByCamunda, workflowInstanceId
        );
    }
}
