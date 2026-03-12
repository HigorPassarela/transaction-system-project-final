package br.com.agibank.extract.service;

import br.com.agibank.extract.model.dto.ExtractResponse;
import br.com.agibank.extract.model.dto.TransactionDTO;
import br.com.agibank.extract.model.entity.TransactionRecord;
import br.com.agibank.extract.pdf.PdfGenerator;
import br.com.agibank.extract.repository.TransactionRecordRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExtractService {

    private static final Logger logger = LoggerFactory.getLogger(ExtractService.class);

    private final TransactionRecordRepository repository;
    private final PdfGenerator pdfGenerator;
    private final NotificationService notificationService;

    @Autowired(required = false) // Opcional para evitar erro de inicialização
    private RuntimeService runtimeService;

    public ExtractService(TransactionRecordRepository repository,
                          PdfGenerator pdfGenerator,
                          NotificationService notificationService) {
        this.repository = repository;
        this.pdfGenerator = pdfGenerator;
        this.notificationService = notificationService;
    }

    @Transactional
    public void registrarTransacao(TransactionDTO transacao) {
        logger.info("=== REGISTRANDO TRANSAÇÃO ===");
        logger.info("ID: {}, Conta: {}, Valor: {}, Tipo: {}",
                transacao.idTransacao(), transacao.numeroConta(), transacao.valor(), transacao.tipo());

        try {
            // Verificar se já existe
            Optional<TransactionRecord> existente = repository.findByTransactionId(transacao.idTransacao());
            if (existente.isPresent()) {
                logger.warn("Transação {} já existe no MongoDB, ignorando duplicata", transacao.idTransacao());
                return;
            }

            // Criar e salvar registro
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

            TransactionRecord saved = repository.save(record);
            logger.info("✅ Transação {} salva no MongoDB com ID: {}", transacao.idTransacao(), saved.getId());

            // Iniciar workflow Camunda
            iniciarWorkflowCamunda(transacao, saved);

        } catch (DuplicateKeyException e) {
            logger.warn("Tentativa de inserir transação duplicada: {}", transacao.idTransacao());
        } catch (Exception e) {
            logger.error("Erro ao salvar transação {} no MongoDB: {}", transacao.idTransacao(), e.getMessage(), e);
            throw new RuntimeException("Falha ao persistir transação", e);
        }
    }

    private void iniciarWorkflowCamunda(TransactionDTO transacao, TransactionRecord record) {
        try {
            logger.info("🚀 Iniciando workflow Camunda para transação: {}", transacao.idTransacao());

            if (runtimeService == null) {
                logger.warn("⚠️ RuntimeService não disponível, usando workflow simulado");
                processarWorkflowSimulado(transacao, record);
                return;
            }

            // Preparar variáveis para o processo
            Map<String, Object> variables = new HashMap<>();
            variables.put("idTransacao", transacao.idTransacao());
            variables.put("numeroConta", transacao.numeroConta());
            variables.put("valor", transacao.valor().toString());
            variables.put("tipo", transacao.tipo());
            variables.put("status", transacao.status());
            variables.put("descricao", transacao.descricao() != null ? transacao.descricao() : "");
            variables.put("dataHora", transacao.dataHora().toString());
            variables.put("saldoAtual", transacao.saldoAtual().toString());
            variables.put("mongoRecordId", record.getId());

            // Iniciar processo
            String processInstanceId = runtimeService.startProcessInstanceByKey(
                    "transaction-notification-process",
                    transacao.idTransacao(), // Business Key
                    variables
            ).getId();

            // Atualizar record
            record.setWorkflowInstanceId(processInstanceId);
            record.setProcessedByCamunda(true);
            repository.save(record);

            logger.info("✅ Workflow Camunda iniciado - Process Instance ID: {}", processInstanceId);

        } catch (Exception e) {
            logger.error("❌ Erro ao iniciar workflow Camunda: {}", e.getMessage(), e);

            // Fallback para workflow simulado
            processarWorkflowSimulado(transacao, record);
        }
    }

    private void processarWorkflowSimulado(TransactionDTO transacao, TransactionRecord record) {
        try {
            logger.info("🔄 Executando workflow simulado para transação: {}", transacao.idTransacao());

            // Simular ID de processo
            String processInstanceId = "SIMULATED-" + System.currentTimeMillis();

            // Atualizar record
            record.setWorkflowInstanceId(processInstanceId);
            record.setProcessedByCamunda(true);
            repository.save(record);

            // Enviar notificação diretamente
            enviarNotificacaoSimulada(transacao);

            logger.info("✅ Workflow simulado concluído - Process Instance ID: {}", processInstanceId);

        } catch (Exception e) {
            logger.error("❌ Erro no workflow simulado: {}", e.getMessage(), e);

            try {
                record.setProcessedByCamunda(false);
                repository.save(record);
            } catch (Exception saveEx) {
                logger.error("❌ Erro ao salvar status de falha: {}", saveEx.getMessage());
            }
        }
    }

    private void enviarNotificacaoSimulada(TransactionDTO transacao) {
        try {
            logger.info("📤 Enviando notificação simulada para transação: {}", transacao.idTransacao());

            // Criar request de notificação
            br.com.agibank.extract.model.dto.NotificationRequest request =
                    new br.com.agibank.extract.model.dto.NotificationRequest(
                            transacao.idTransacao(),
                            transacao.numeroConta(),
                            transacao.valor(),
                            transacao.tipo(),
                            transacao.status(),
                            transacao.descricao(),
                            transacao.dataHora(),
                            transacao.saldoAtual()
                    );

            boolean sucesso = notificationService.enviarNotificacao(request);

            if (sucesso) {
                logger.info("✅ Notificação simulada enviada com sucesso");
            } else {
                logger.warn("⚠️ Falha ao enviar notificação simulada");
            }

        } catch (Exception e) {
            logger.error("❌ Erro ao enviar notificação simulada: {}", e.getMessage(), e);
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
                .sorted((t1, t2) -> t2.dataHora().compareTo(t1.dataHora()))
                .toList();

        BigDecimal saldoFinal = transacoes.isEmpty() ? BigDecimal.ZERO :
                transacoes.stream()
                        .max((t1, t2) -> t1.getDataHora().compareTo(t2.getDataHora()))
                        .map(TransactionRecord::getSaldoAtual)
                        .orElse(BigDecimal.ZERO);

        BigDecimal saldoInicial = calcularSaldoInicial(transacoes, saldoFinal);

        return new ExtractResponse(
                numeroConta,
                dataInicial,
                dataFinal,
                saldoInicial,
                saldoFinal,
                transacoesDto,
                transacoesDto.size(),
                LocalDateTime.now()
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

    public long contarTransacoes(String numeroConta) {
        return repository.countByNumeroConta(numeroConta);
    }

    public boolean transacaoJaProcessada(String transactionId) {
        return repository.findByTransactionId(transactionId).isPresent();
    }

    // Métodos para monitoramento do Camunda
    public List<TransactionRecord> buscarTransacoesPendentesWorkflow() {
        return repository.findByProcessedByCamundaFalse();
    }

    public Optional<TransactionRecord> buscarPorWorkflowId(String workflowInstanceId) {
        return repository.findByWorkflowInstanceId(workflowInstanceId);
    }

    // Método para verificar status do Camunda
    public Map<String, Object> getStatusCamunda() {
        Map<String, Object> status = new HashMap<>();
        status.put("runtimeServiceDisponivel", runtimeService != null);
        status.put("timestamp", LocalDateTime.now());

        if (runtimeService != null) {
            try {
                // Testar se consegue acessar o Camunda
                long count = runtimeService.createProcessInstanceQuery().count();
                status.put("processosAtivos", count);
                status.put("status", "CONNECTED");
            } catch (Exception e) {
                status.put("status", "ERROR");
                status.put("erro", e.getMessage());
            }
        } else {
            status.put("status", "NOT_AVAILABLE");
        }

        return status;
    }

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

        List<TransactionRecord> transacoesOrdenadas = transacoes.stream()
                .sorted((t1, t2) -> t2.getDataHora().compareTo(t1.getDataHora()))
                .toList();

        for (TransactionRecord transacao : transacoesOrdenadas) {
            if ("DEBITO".equals(transacao.getTipo())) {
                saldoCalculado = saldoCalculado.add(transacao.getValor());
            } else {
                saldoCalculado = saldoCalculado.subtract(transacao.getValor());
            }
        }

        return saldoCalculado;
    }
}
