package br.com.agibank.notifications.controller;

import br.com.agibank.notifications.sse.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public ResponseEntity<Map<String, Object>> enviarNotificacao(@RequestBody Map<String, Object> request) {
        logger.info("📨 Recebida solicitação de notificação do Camunda: {}", request);

        try {
            // Extrair dados do request
            String numeroConta = (String) request.get("numeroConta");
            String idTransacao = (String) request.get("idTransacao");
            String tipo = (String) request.get("tipo");

            // ✅ CORREÇÃO: Tratar valores que podem vir como Double
            String valor = convertToString(request.get("valor"));
            String saldoAtual = convertToString(request.get("saldoAtual"));

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
            String mensagem = String.format("%s Transação %s de R$ %s processada. Saldo: R$ %s",
                    tipoIcon, tipo, valor, saldoAtual);

            // Criar objeto de notificação
            Map<String, Object> notificacao = new HashMap<>();
            notificacao.put("id", idTransacao);
            notificacao.put("numeroConta", numeroConta);
            notificacao.put("titulo", tipo + " Processado");
            notificacao.put("mensagem", mensagem);
            notificacao.put("tipo", tipo);
            notificacao.put("valor", valor);
            notificacao.put("timestamp", LocalDateTime.now());

            // Enviar via SSE
            sseService.enviarNotificacaoGenerica(numeroConta, notificacao);

            logger.info("✅ Notificação enviada com sucesso para conta {} - transação {}", numeroConta, idTransacao);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Notificação enviada com sucesso");
            response.put("numeroConta", numeroConta);
            response.put("idTransacao", idTransacao);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Erro ao processar notificação: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Erro ao processar notificação: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    // ✅ ADICIONE ESTE MÉTODO HELPER
    private String convertToString(Object value) {
        if (value == null) return "0.00";
        if (value instanceof String) return (String) value;
        if (value instanceof Double) return String.format("%.2f", (Double) value);
        if (value instanceof BigDecimal) return ((BigDecimal) value).toString();
        if (value instanceof Number) return String.format("%.2f", ((Number) value).doubleValue());
        return value.toString();
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
