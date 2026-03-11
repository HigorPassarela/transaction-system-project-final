package br.com.agibank.extract.controller;

import br.com.agibank.extract.model.dto.ExtractRequest;
import br.com.agibank.extract.model.dto.ExtractResponse;
import br.com.agibank.extract.model.dto.TransactionDTO;
import br.com.agibank.extract.service.ExtractService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/extratos")
@Tag(name = "Extratos", description = "API para geração de extratos bancários")
public class ExtractController {

    private static final Logger logger = LoggerFactory.getLogger(ExtractController.class);

    private final ExtractService extractService;

    public ExtractController(ExtractService extractService) {
        this.extractService = extractService;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
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
    @Operation(summary = "Consultar últimas transações", description = "Consulta as últimas N transações de uma conta")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transações consultadas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
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
        logger.info("Encontradas {} transações para conta {}", transacoes.size(), numeroConta);

        return ResponseEntity.ok(transacoes);
    }

    @GetMapping("/count/{numeroConta}")
    @Operation(summary = "Contar transações", description = "Conta o total de transações de uma conta")
    public ResponseEntity<Long> contarTransacoes(
            @Parameter(description = "Número da conta", example = "12345678")
            @PathVariable String numeroConta) {

        logger.info("Contando transações da conta {}", numeroConta);

        long count = extractService.contarTransacoes(numeroConta);
        logger.info("Conta {} possui {} transações", numeroConta, count);

        return ResponseEntity.ok(count);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica se o serviço está funcionando")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Extract Service está funcionando!");
    }
}
