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
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Notifications Service está funcionando!");
    }
}
