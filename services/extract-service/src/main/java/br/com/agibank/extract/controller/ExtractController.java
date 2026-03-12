package br.com.agibank.extract.controller;

import br.com.agibank.extract.model.dto.ExtractRequest;
import br.com.agibank.extract.model.dto.ExtractResponse;
import br.com.agibank.extract.model.dto.TransactionDTO;
import br.com.agibank.extract.model.entity.TransactionRecord;
import br.com.agibank.extract.service.ExtractService;
import br.com.agibank.extract.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/extratos")
@Tag(name = "Extratos", description = "API para geração de extratos bancários com workflow Camunda")
public class ExtractController {

    private static final Logger logger = LoggerFactory.getLogger(ExtractController.class);

    private final ExtractService extractService;
    private final NotificationService notificationService;

    public ExtractController(ExtractService extractService,
                             NotificationService notificationService) {
        this.extractService = extractService;
        this.notificationService = notificationService;
    }

    @PostMapping
    @Operation(summary = "Gerar extrato", description = "Gera extrato bancário por período")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Extrato gerado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<ExtractResponse> gerarExtrato(@Valid @RequestBody ExtractRequest request) {
        logger.info("Solicitação de extrato recebida: conta={}, período={} a {}",
                request.numeroConta(), request.dataInicial(), request.dataFinal());

        try {
            ExtractResponse extrato = extractService.gerarExtrato(
                    request.numeroConta(),
                    request.dataInicial(),
                    request.dataFinal()
            );

            logger.info("Extrato gerado com sucesso: {} transações encontradas", extrato.totalTransacoes());
            return ResponseEntity.ok(extrato);

        } catch (Exception e) {
            logger.error("Erro ao gerar extrato para conta {}: {}", request.numeroConta(), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/pdf")
    @Operation(summary = "Gerar extrato PDF", description = "Gera extrato bancário em formato PDF")
    public ResponseEntity<byte[]> gerarExtratoPdf(@Valid @RequestBody ExtractRequest request) {
        logger.info("Solicitação de PDF do extrato: conta={}, período={} a {}",
                request.numeroConta(), request.dataInicial(), request.dataFinal());

        try {
            byte[] pdf = extractService.gerarExtratoPdf(
                    request.numeroConta(),
                    request.dataInicial(),
                    request.dataFinal()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "extrato_" + request.numeroConta() + "_" +
                            request.dataInicial() + "_" + request.dataFinal() + ".pdf");

            logger.info("PDF do extrato gerado com sucesso para conta {}", request.numeroConta());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf);

        } catch (Exception e) {
            logger.error("Erro ao gerar PDF do extrato para conta {}: {}", request.numeroConta(), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/ultimas/{numeroConta}")
    @Operation(summary = "Consultar últimas transações")
    public ResponseEntity<List<TransactionDTO>> consultarUltimasTransacoes(
            @Parameter(description = "Número da conta", example = "12345678")
            @PathVariable String numeroConta,
            @Parameter(description = "Limite de transações", example = "10")
            @RequestParam(defaultValue = "10") int limite) {

        logger.info("Consultando últimas {} transações da conta {}", limite, numeroConta);

        if (limite <= 0 || limite > 100) {
            return ResponseEntity.badRequest().build();
        }

        List<TransactionDTO> transacoes = extractService.consultarUltimasTransacoes(numeroConta, limite);
        return ResponseEntity.ok(transacoes);
    }

    @GetMapping("/count/{numeroConta}")
    @Operation(summary = "Contar transações")
    public ResponseEntity<Map<String, Object>> contarTransacoes(
            @Parameter(description = "Número da conta", example = "12345678")
            @PathVariable String numeroConta) {

        long count = extractService.contarTransacoes(numeroConta);

        Map<String, Object> response = new HashMap<>();
        response.put("numeroConta", numeroConta);
        response.put("totalTransacoes", count);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    // === ENDPOINTS DO CAMUNDA ===

    @GetMapping("/camunda/status")
    @Operation(summary = "Status do Camunda", description = "Verifica status da conexão com Camunda")
    public ResponseEntity<Map<String, Object>> statusCamunda() {
        logger.info("Verificando status do Camunda");

        Map<String, Object> status = extractService.getStatusCamunda();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/camunda/transacoes-pendentes")
    @Operation(summary = "Transações pendentes de workflow")
    public ResponseEntity<List<TransactionRecord>> transacoesPendentes() {
        logger.info("Buscando transações pendentes de processamento pelo Camunda");

        List<TransactionRecord> pendentes = extractService.buscarTransacoesPendentesWorkflow();

        logger.info("Encontradas {} transações pendentes", pendentes.size());
        return ResponseEntity.ok(pendentes);
    }

    @PostMapping("/teste-notificacao")
    @Operation(summary = "Testar serviço de notificações")
    public ResponseEntity<Map<String, Object>> testarNotificacao() {
        logger.info("Testando conexão com serviço de notificações");

        try {
            boolean sucesso = notificationService.testarConexao();

            Map<String, Object> response = new HashMap<>();
            response.put("servicoNotificacoes", sucesso ? "ONLINE" : "OFFLINE");
            response.put("timestamp", LocalDateTime.now());
            response.put("url", "http://localhost:8083");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao testar notificação: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("servicoNotificacoes", "ERRO");
            response.put("erro", e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Extract Service");
        health.put("camunda", extractService.getStatusCamunda());
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");

        return ResponseEntity.ok(health);
    }
}
