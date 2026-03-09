package br.com.agibank.transaction.service;

import br.com.agibank.transaction.messaging.dto.TransacaoEvento;
import br.com.agibank.transaction.messaging.producer.TransacaoProducer;
import br.com.agibank.transaction.model.dto.*;
import br.com.agibank.transaction.model.entity.SaldoConta;
import br.com.agibank.transaction.repository.SaldoContaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(TransacaoService.class);

    private final SaldoContaRepository saldoContaRepository;
    private final TransacaoProducer transacaoProducer;

    public TransacaoService(SaldoContaRepository saldoContaRepository,
                            TransacaoProducer transacaoProducer) {
        this.saldoContaRepository = saldoContaRepository;
        this.transacaoProducer = transacaoProducer;
    }

    @Transactional
    public TransacaoResponse processarTransacao(TransacaoRequest solicitacao) {
        logger.info("Processando transação: conta={}, tipo={}, valor={}",
                solicitacao.numeroConta(), solicitacao.tipo(), solicitacao.valor());

        String idTransacao = gerarIdTransacao();

        // Buscar ou criar saldo da conta
        SaldoConta saldoConta = buscarOuCriarSaldoConta(solicitacao.numeroConta());

        // Validar e processar transação
        if (solicitacao.isDebito()) {
            return processarDebito(idTransacao, solicitacao, saldoConta);
        } else {
            return processarCredito(idTransacao, solicitacao, saldoConta);
        }
    }

    private TransacaoResponse processarDebito(String idTransacao,
                                              TransacaoRequest solicitacao,
                                              SaldoConta saldoConta) {

        if (!saldoConta.podeDebitar(solicitacao.valor())) {
            logger.warn("Débito rejeitado - saldo insuficiente: conta={}, saldo={}, valor={}",
                    solicitacao.numeroConta(), saldoConta.getSaldo(), solicitacao.valor());

            return TransacaoResponse.rejeitada(
                    idTransacao,
                    solicitacao.numeroConta(),
                    solicitacao.valor(),
                    solicitacao.tipo(),
                    "Saldo insuficiente"
            );
        }

        saldoConta.debitar(solicitacao.valor());
        saldoContaRepository.save(saldoConta);

        TransacaoEvento evento = TransacaoEvento.transacaoAprovada(
                idTransacao,
                solicitacao.numeroConta(),
                solicitacao.valor(),
                solicitacao.tipo(),
                solicitacao.descricao(),
                saldoConta.getSaldo()
        );

        transacaoProducer.enviarTransacaoAprovada(evento);

        logger.info("Débito processado com sucesso: transacao={}, novoSaldo={}",
                idTransacao, saldoConta.getSaldo());

        return TransacaoResponse.aprovada(
                idTransacao,
                solicitacao.numeroConta(),
                solicitacao.valor(),
                solicitacao.tipo(),
                saldoConta.getSaldo()
        );
    }

    private TransacaoResponse processarCredito(String idTransacao,
                                               TransacaoRequest solicitacao,
                                               SaldoConta saldoConta) {

        if (!saldoConta.podeCreditar(solicitacao.valor())) {
            logger.warn("Crédito rejeitado - limite insuficiente: conta={}, limite={}, valor={}",
                    solicitacao.numeroConta(), saldoConta.getLimiteCredito(), solicitacao.valor());

            return TransacaoResponse.rejeitada(
                    idTransacao,
                    solicitacao.numeroConta(),
                    solicitacao.valor(),
                    solicitacao.tipo(),
                    "Limite de crédito insuficiente"
            );
        }

        saldoConta.creditar(solicitacao.valor());
        saldoContaRepository.save(saldoConta);

        TransacaoEvento evento = TransacaoEvento.transacaoAprovada(
                idTransacao,
                solicitacao.numeroConta(),
                solicitacao.valor(),
                solicitacao.tipo(),
                solicitacao.descricao(),
                saldoConta.getSaldo()
        );

        transacaoProducer.enviarTransacaoAprovada(evento);

        logger.info("Crédito processado com sucesso: transacao={}, novoSaldo={}",
                idTransacao, saldoConta.getSaldo());

        return TransacaoResponse.aprovada(
                idTransacao,
                solicitacao.numeroConta(),
                solicitacao.valor(),
                solicitacao.tipo(),
                saldoConta.getSaldo()
        );
    }

    private SaldoConta buscarOuCriarSaldoConta(String numeroConta) {
        return saldoContaRepository.findByNumeroConta(numeroConta)
                .orElseGet(() -> {
                    logger.info("Criando nova conta: {}", numeroConta);
                    SaldoConta novaConta = new SaldoConta(
                            numeroConta,
                            new BigDecimal("1000.00"), // Saldo inicial
                            new BigDecimal("5000.00")   // Limite de crédito inicial
                    );
                    return saldoContaRepository.save(novaConta);
                });
    }

    private String gerarIdTransacao() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public SaldoConta consultarSaldo(String numeroConta) {
        return saldoContaRepository.findByNumeroConta(numeroConta)
                .orElse(null);
    }
}
