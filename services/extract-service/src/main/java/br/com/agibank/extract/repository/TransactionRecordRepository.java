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

    // Consulta simples primeiro
    List<TransactionRecord> findByNumeroConta(String numeroConta);

    // Consulta com período
    @Query("{'numeroConta': ?0, 'dataHora': {$gte: ?1, $lte: ?2}}")
    List<TransactionRecord> findByNumeroContaAndDataHoraBetween(
            String numeroConta,
            LocalDateTime dataInicial,
            LocalDateTime dataFinal
    );

    Page<TransactionRecord> findByNumeroContaOrderByDataHoraDesc(String numeroConta, Pageable pageable);

    Optional<TransactionRecord> findByTransactionId(String transactionId);

    long countByNumeroConta(String numeroConta);

    @Query(value = "{'numeroConta': ?0}", sort = "{'dataHora': -1}")
    List<TransactionRecord> findTopNByNumeroConta(String numeroConta, Pageable pageable);
}
