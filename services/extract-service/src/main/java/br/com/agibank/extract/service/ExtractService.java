package br.com.agibank.extract.service;

import br.com.agibank.extract.model.dto.ExtractResponse;
import br.com.agibank.extract.model.dto.TransactionDTO;
import br.com.agibank.extract.model.entity.TransactionRecord;
import br.com.agibank.extract.pdf.PdfGenerator;
import br.com.agibank.extract.repository.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ExtractService {

    private static final Logger logger = LoggerFactory.getLogger(ExtractService.class);

    private final TransactionRecordRepository repository;
    private final PdfGenerator pdfGenerator;

    public ExtractService(TransactionRecordRepository repository, PdfGenerator pdfGenerator) {
        this.repository = repository;
        this.pdfGenerator = pdfGenerator;
    }

    @Transactional
    public void registrarTransacao(TransactionDTO transacao) {
        logger.info("=== DEBUG REGISTRO TRANSAÇÃO ===");
        logger.info("Dados recebidos do Kafka:");
        logger.info("  - ID: {}", transacao.idTransacao());
        logger.info("  - Conta: {}", transacao.numeroConta());
        logger.info("  - Valor: {}", transacao.valor());
        logger.info("  - Tipo: {}", transacao.tipo());
        logger.info("  - Status: {}", transacao.status());
        logger.info("  - Descrição: {}", transacao.descricao());
        logger.info("  - Data/Hora: {}", transacao.dataHora());
        logger.info("  - Saldo Atual: {}", transacao.saldoAtual());

        try {
            Optional<TransactionRecord> existente = repository.findByTransactionId(transacao.idTransacao());
            if (existente.isPresent()) {
                logger.warn("Transação {} já existe no MongoDB, ignorando duplicata", transacao.idTransacao());
                return;
            }

            logger.info("Criando TransactionRecord...");
            TransactionRecord record = new TransactionRecord(
                    transacao.idTransacao(),
                    transacao.numeroConta(),
                    transacao.valor(),
                    transacao.tipo(),
                    transacao.descricao(),
                    transacao.status(),
                    transacao.dataHora(),
                    transacao.saldoAtual()
            );

            logger.info("Dados do TransactionRecord antes de salvar:");
            logger.info("  - TransactionId: {}", record.getTransactionId());
            logger.info("  - NumeroConta: {}", record.getNumeroConta());
            logger.info("  - Valor: {}", record.getValor());
            logger.info("  - Tipo: {}", record.getTipo());

            TransactionRecord saved = repository.save(record);
            logger.info("Transação {} salva no MongoDB com ID: {}", transacao.idTransacao(), saved.getId());

            // Verificar se foi salvo corretamente
            logger.info("Dados salvos no MongoDB:");
            logger.info("  - ID MongoDB: {}", saved.getId());
            logger.info("  - TransactionId: {}", saved.getTransactionId());
            logger.info("  - NumeroConta: {}", saved.getNumeroConta());
            logger.info("  - Valor: {}", saved.getValor());

        } catch (DuplicateKeyException e) {
            logger.warn("Tentativa de inserir transação duplicada: {}", transacao.idTransacao());
        } catch (Exception e) {
            logger.error("Erro ao salvar transação {} no MongoDB: {}", transacao.idTransacao(), e.getMessage(), e);
            throw new RuntimeException("Falha ao persistir transação", e);
        }
    }

    public ExtractResponse gerarExtrato(String numeroConta, LocalDate dataInicial, LocalDate dataFinal) {
        logger.info("Gerando extrato para conta {} de {} até {}", numeroConta, dataInicial, dataFinal);

        LocalDateTime inicio = dataInicial.atStartOfDay();
        LocalDateTime fim = dataFinal.atTime(LocalTime.MAX);

        List<TransactionRecord> transacoes = repository.findByNumeroContaAndDataHoraBetween(
                numeroConta, inicio, fim);

        logger.info("Encontradas {} transações para conta {} no período", transacoes.size(), numeroConta);

        List<TransactionDTO> transacoesDto = transacoes.stream()
                .map(this::toDto)
                .toList();

        BigDecimal saldoFinal = transacoes.isEmpty() ? BigDecimal.ZERO :
                transacoes.get(transacoes.size() - 1).getSaldoAtual();

        BigDecimal saldoInicial = calcularSaldoInicial(transacoes, saldoFinal);

        return new ExtractResponse(
                numeroConta,
                dataInicial,
                dataFinal,
                saldoInicial,
                saldoFinal,
                transacoesDto,
                transacoesDto.size()
        );
    }

    public byte[] gerarExtratoPdf(String numeroConta, LocalDate dataInicial, LocalDate dataFinal) {
        logger.info("Gerando PDF do extrato para conta {}", numeroConta);
        ExtractResponse extrato = gerarExtrato(numeroConta, dataInicial, dataFinal);
        return pdfGenerator.gerarPdf(extrato);
    }

    public List<TransactionDTO> consultarUltimasTransacoes(String numeroConta, int limite) {
        logger.info("Consultando últimas {} transações da conta {}", limite, numeroConta);

        List<TransactionRecord> transacoes = repository.findTopNByNumeroConta(
                numeroConta, PageRequest.of(0, limite));

        return transacoes.stream()
                .map(this::toDto)
                .toList();
    }

    public boolean transacaoJaProcessada(String transactionId) {
        return repository.findByTransactionId(transactionId).isPresent();
    }

    public long contarTransacoes(String numeroConta) {
        logger.info("=== DEBUG MONGODB ===");
        logger.info("Contando transações para conta: [{}]", numeroConta);

        // Teste 1: Contar todos os registros
        long totalGeral = repository.count();
        logger.info("Total geral de transações no MongoDB: {}", totalGeral);

        // Teste 2: Listar todas as contas que existem (máximo 10 para não poluir log)
        List<TransactionRecord> todasTransacoes = repository.findAll();
        logger.info("Total de transações encontradas: {}", todasTransacoes.size());
        logger.info("Contas encontradas no MongoDB:");

        int contador = 0;
        for (TransactionRecord tr : todasTransacoes) {
            if (contador < 10) { // Limitar para não poluir o log
                logger.info("  - ID: {}, Conta: [{}], Valor: {}, Tipo: {}",
                        tr.getId(), tr.getNumeroConta(), tr.getValor(), tr.getTipo());
            }
            contador++;
        }

        if (todasTransacoes.size() > 10) {
            logger.info("  ... e mais {} transações", todasTransacoes.size() - 10);
        }

        // Teste 3: Buscar especificamente pela conta
        List<TransactionRecord> transacoesDaConta = repository.findByNumeroConta(numeroConta);
        logger.info("Transações encontradas para conta [{}]: {}", numeroConta, transacoesDaConta.size());

        for (TransactionRecord tr : transacoesDaConta) {
            logger.info("  -> Transação: ID={}, Valor={}, Data={}",
                    tr.getTransactionId(), tr.getValor(), tr.getDataHora());
        }

        // Teste 4: Contar pela conta específica
        long count = repository.countByNumeroConta(numeroConta);
        logger.info("Count final para conta [{}]: {}", numeroConta, count);

        return count;
    }

//    public long contarTransacoes(String numeroConta) {
//        return repository.countByNumeroConta(numeroConta);
//    }

    private TransactionDTO toDto(TransactionRecord record) {
        return new TransactionDTO(
                record.getTransactionId(),
                record.getNumeroConta(),
                record.getValor(),
                record.getTipo(),
                record.getStatus(),
                record.getDescricao(),
                record.getDataHora(),
                record.getSaldoAtual()
        );
    }

    private BigDecimal calcularSaldoInicial(List<TransactionRecord> transacoes, BigDecimal saldoFinal) {
        if (transacoes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal saldoCalculado = saldoFinal;
        for (int i = transacoes.size() - 1; i >= 0; i--) {
            TransactionRecord transacao = transacoes.get(i);
            if ("DEBITO".equals(transacao.getTipo())) {
                saldoCalculado = saldoCalculado.add(transacao.getValor());
            } else {
                saldoCalculado = saldoCalculado.subtract(transacao.getValor());
            }
        }

        return saldoCalculado;
    }
}
