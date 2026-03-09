package br.com.agibank.transaction.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidatorExceptions(MethodArgumentNotValidException ex) {
        logger.warn("Erro de validação: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) errors).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse erro = new ErrorResponse(
                "VALIDATION_ERROR",
                "Dados Inválidos",
                errors.toString(),
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(TransacaoException.class)
    public ResponseEntity<ErrorResponse> handleTransacaoException(TransacaoException ex) {
        logger.error("Erro de transação: {}", ex.getMessage());

        ErrorResponse erro = new ErrorResponse(
                "TRANSACTION_ERROR",
                ex.getMessage(),
                ex.getDetalhes(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(erro);
    }

    @ExceptionHandler(ContaNaoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleContaNaoEncontradaException(ContaNaoEncontradaException ex) {
        logger.error("Conta não encontrada: {}", ex.getMessage());

        ErrorResponse erro = new ErrorResponse(
                "ACCOUNT_NOT_FOUND",
                ex.getMessage(),
                ex.getNumeroConta(),
                LocalDateTime.now()
        );

        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Erro interno não tratado", ex);

        ErrorResponse erro = new ErrorResponse(
                "INTERNAL_ERROR",
                "Erro interno do servidor",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
