package br.com.agibank.extract.repository;

import br.com.agibank.extract.model.entity.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRecordRepository extends MongoRepository<TransactionRecord, String> {

    // Busca básica por conta
    List<TransactionRecord> findByNumeroConta(String numeroConta);

    // Busca por período
    @Query("{'numeroConta': ?0, 'dataHora': {$gte: ?1, $lte: ?2}}")
    List<TransactionRecord> findByNumeroContaAndDataHoraBetween(
            String numeroConta,
            LocalDateTime dataInicial,
            LocalDateTime dataFinal
    );

    // Busca paginada ordenada por data
    Page<TransactionRecord> findByNumeroContaOrderByDataHoraDesc(String numeroConta, Pageable pageable);

    // Busca por ID da transação (único)
    Optional<TransactionRecord> findByTransactionId(String transactionId);

    // Contagem por conta
    long countByNumeroConta(String numeroConta);

    // Últimas N transações de uma conta
    @Query(value = "{'numeroConta': ?0}", sort = "{'dataHora': -1}")
    List<TransactionRecord> findTopNByNumeroConta(String numeroConta, Pageable pageable);

    // === MÉTODOS PARA CAMUNDA (ADICIONAR ESTES) ===

    // Buscar transações não processadas pelo Camunda
    List<TransactionRecord> findByProcessedByCamundaFalse();

    // Buscar por ID do workflow
    Optional<TransactionRecord> findByWorkflowInstanceId(String workflowInstanceId);

    // Buscar transações processadas pelo Camunda
    @Query("{'processedByCamunda': true, 'workflowInstanceId': {$exists: true}}")
    List<TransactionRecord> findProcessedByCamunda();

    // Outros métodos existentes...
    List<TransactionRecord> findByNumeroContaAndTipo(String numeroConta, String tipo);
    List<TransactionRecord> findByStatus(String status);

    @Query("{'dataHora': {$gte: ?0, $lte: ?1}}")
    List<TransactionRecord> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    @Query("{'numeroConta': ?0, 'valor': {$gte: ?1, $lte: ?2}}")
    List<TransactionRecord> findByNumeroContaAndValorBetween(
            String numeroConta,
            java.math.BigDecimal valorMin,
            java.math.BigDecimal valorMax
    );

    @Query(value = "{'numeroConta': ?0, 'tipo': ?1}", count = true)
    long countByNumeroContaAndTipo(String numeroConta, String tipo);
}
