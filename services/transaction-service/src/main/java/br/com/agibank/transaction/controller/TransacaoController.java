package br.com.agibank.transaction.controller;

import br.com.agibank.transaction.model.dto.SaldoResponse;
import br.com.agibank.transaction.model.dto.TransacaoRequest;
import br.com.agibank.transaction.model.dto.TransacaoResponse;
import br.com.agibank.transaction.model.entity.SaldoConta;
import br.com.agibank.transaction.service.TransacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transacoes")
@Tag(name = "Transações", description = "API para processamento de transações bancárias")
public class TransacaoController {

    private static final Logger logger = LoggerFactory.getLogger(TransacaoController.class);

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @PostMapping
    @Operation(summary = "Processar transação", description = "Processa uma nova transação de débito ou crédito")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transação processada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "422", description = "Transação rejeitada por regra de negócio"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<TransacaoResponse> processarTransacao(
            @Valid @RequestBody TransacaoRequest solicitacao) {

        logger.info("Recebida solicitação de transação: conta={}, tipo={}, valor={}",
                solicitacao.numeroConta(), solicitacao.tipo(), solicitacao.valor());

        try {
            TransacaoResponse resposta = transacaoService.processarTransacao(solicitacao);

            if (resposta.isRejeitada()) {
                logger.warn("Transação rejeitada: {}", resposta.mensagem());
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(resposta);
            }

            logger.info("Transação processada com sucesso: {}", resposta.idTransacao());
            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            logger.error("Erro ao processar transação", e);
            throw e;
        }
    }

    @GetMapping("/saldo/{numeroConta}")
    @Operation(summary = "Consultar saldo", description = "Consulta o saldo atual de uma conta")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo consultado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    })
    public ResponseEntity<SaldoResponse> consultarSaldo(
            @Parameter(description = "Número da conta", example = "12345678")
            @PathVariable String numeroConta) {

        logger.info("Consultando saldo da conta: {}", numeroConta);

        SaldoConta saldoConta = transacaoService.consultarSaldo(numeroConta);

        if (saldoConta == null) {
            logger.warn("Conta não encontrada: {}", numeroConta);
            return ResponseEntity.notFound().build();
        }

        SaldoResponse response = new SaldoResponse(
                saldoConta.getNumeroConta(),
                saldoConta.getSaldo(),
                saldoConta.getLimiteCredito(),
                saldoConta.getUltimaAtualizacao()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica se o serviço está funcionando")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Transaction Service está funcionando!");
    }
}
