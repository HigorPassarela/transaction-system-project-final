package br.com.agibank.notifications.controller;

import br.com.agibank.notifications.sse.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notificações", description = "API para notificações em tempo real")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final SseService sseService;

    public NotificationController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping(value = "/stream/{numeroConta}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Conectar ao stream de notificações",
            description = "Estabelece conexão SSE para receber notificações em tempo real")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conexão estabelecida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Número da conta inválido")
    })
    public SseEmitter conectar(
            @Parameter(description = "Número da conta", example = "12345678")
            @PathVariable String numeroConta) {

        logger.info("Solicitação de conexão SSE para conta: {}", numeroConta);

        if (numeroConta == null || numeroConta.trim().isEmpty()) {
            throw new IllegalArgumentException("Número da conta é obrigatório");
        }

        return sseService.conectar(numeroConta.trim());
    }

    @PostMapping("/send")
    @Operation(summary = "Enviar notificação", description = "Recebe notificação do Camunda e envia via SSE")
    public ResponseEntity<Map<String, Object>> enviarNotificacao(@RequestBody Map<String, Object> payload) {

        logger.info("📨 Recebida solicitação de notificação do Camunda: {}", payload);

        try {
            // Extrair dados do payload com conversão segura
            String idTransacao = (String) payload.get("idTransacao");
            String numeroConta = (String) payload.get("numeroConta");

            // ✅ CORREÇÃO: Converter Double/Number para String de forma segura
            String tipo = (String) payload.get("tipo");
            String status = (String) payload.get("status");

            // Converter valores numéricos de forma segura
            BigDecimal valor = convertToBigDecimal(payload.get("valor"));
            BigDecimal saldoAtual = convertToBigDecimal(payload.get("saldoAtual"));

            // Converter dataHora de forma segura
            LocalDateTime dataHora = convertToLocalDateTime(payload.get("dataHora"));

            // Validações básicas
            if (numeroConta == null || idTransacao == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "ERROR");
                errorResponse.put("message", "Dados obrigatórios ausentes (numeroConta, idTransacao)");
                errorResponse.put("timestamp", LocalDateTime.now());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Criar mensagem de notificação
            String tipoIcon = "DEBITO".equals(tipo) ? "🔻" : "🔺";
            String mensagem = String.format("%s Transação %s de R$ %.2f processada. Saldo: R$ %.2f",
                    tipoIcon, tipo, valor, saldoAtual);

            // Criar objeto de notificação
            Map<String, Object> notificacao = new HashMap<>();
            notificacao.put("id", idTransacao);
            notificacao.put("numeroConta", numeroConta);
            notificacao.put("titulo", tipo + " Processado");
            notificacao.put("mensagem", mensagem);
            notificacao.put("tipo", tipo);
            notificacao.put("valor", valor);
            notificacao.put("timestamp", dataHora != null ? dataHora : LocalDateTime.now());

            // Enviar via SSE
            sseService.enviarNotificacaoGenerica(numeroConta, notificacao);

            logger.info("✅ Notificação enviada com sucesso para conta {} - transação {}", numeroConta, idTransacao);

            // Resposta de sucesso
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Notificação enviada com sucesso");
            response.put("transactionId", idTransacao);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Erro ao processar notificação: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Erro interno: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ✅ Métodos auxiliares para conversão segura
    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Double) return BigDecimal.valueOf((Double) value);
        if (value instanceof Integer) return BigDecimal.valueOf((Integer) value);
        if (value instanceof String) return new BigDecimal((String) value);
        return BigDecimal.ZERO;
    }

    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null) return LocalDateTime.now();
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof String) return LocalDateTime.parse((String) value);
        // Se for array (como vem do Camunda), converter
        if (value instanceof java.util.List) {
            java.util.List<?> dateArray = (java.util.List<?>) value;
            if (dateArray.size() >= 6) {
                return LocalDateTime.of(
                        ((Number) dateArray.get(0)).intValue(), // ano
                        ((Number) dateArray.get(1)).intValue(), // mês
                        ((Number) dateArray.get(2)).intValue(), // dia
                        ((Number) dateArray.get(3)).intValue(), // hora
                        ((Number) dateArray.get(4)).intValue(), // minuto
                        ((Number) dateArray.get(5)).intValue(), // segundo
                        dateArray.size() > 6 ? ((Number) dateArray.get(6)).intValue() : 0 // nano
                );
            }
        }
        return LocalDateTime.now();
    }

    @GetMapping("/status/{numeroConta}")
    @Operation(summary = "Status das conexões", description = "Verifica quantas conexões ativas existem para uma conta")
    public ResponseEntity<String> statusConexoes(
            @Parameter(description = "Número da conta", example = "12345678")
            @PathVariable String numeroConta) {

        int conexoes = sseService.getConexoesAtivas(numeroConta);
        return ResponseEntity.ok(String.format("Conta %s possui %d conexões ativas", numeroConta, conexoes));
    }

    @GetMapping("/status/total")
    @Operation(summary = "Total de conexões", description = "Verifica o total de conexões ativas no sistema")
    public ResponseEntity<String> statusTotal() {
        int total = sseService.getTotalConexoes();
        return ResponseEntity.ok(String.format("Total de conexões ativas: %d", total));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica se o serviço está funcionando")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Notifications Service");
        health.put("timestamp", LocalDateTime.now());
        health.put("totalConnections", sseService.getTotalConexoes());

        return ResponseEntity.ok(health);
    }
}
