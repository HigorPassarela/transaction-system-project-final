package br.com.agibank.transaction.repository;

import br.com.agibank.transaction.model.entity.SaldoConta;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SaldoContaRepository extends CrudRepository<SaldoConta, String> {

//    Optional<SaldoConta> findByNumeroConta(String numeroConta);
//
//    boolean existsByNumeroConta(String numeroConta);
}
