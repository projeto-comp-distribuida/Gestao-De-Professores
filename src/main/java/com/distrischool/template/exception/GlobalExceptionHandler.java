package com.distrischool.template.exception;

import com.distrischool.template.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler global de exceções.
 * Centraliza o tratamento de erros e fornece respostas padronizadas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> details = new HashMap<>();
        details.put("error", "ValidationError");
        details.put("path", request.getRequestURI());
        details.put("fields", fieldErrors);

        log.warn("Erro de validação em {}: {}", request.getRequestURI(), fieldErrors);

        ApiResponse<Map<String, Object>> body = ApiResponse.error(details, "A requisição contém campos inválidos.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Recurso não encontrado em {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("error", "ResourceNotFound");
        details.put("path", request.getRequestURI());

        ApiResponse<Map<String, Object>> body = ApiResponse.error(details, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Erro de negócio em {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("error", "BusinessError");
        details.put("path", request.getRequestURI());

        ApiResponse<Map<String, Object>> body = ApiResponse.error(details, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String msg = String.format("Parâmetro '%s' com valor '%s' é inválido para o tipo %s.",
                ex.getName(), ex.getValue(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconhecido");

        Map<String, Object> details = new HashMap<>();
        details.put("error", "TypeMismatch");
        details.put("path", request.getRequestURI());
        details.put("parameter", ex.getName());
        details.put("value", ex.getValue());

        log.warn("TypeMismatch em {}: {}", request.getRequestURI(), msg);

        ApiResponse<Map<String, Object>> body = ApiResponse.error(details, msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        Map<String, Object> details = new HashMap<>();
        details.put("error", "MalformedJson");
        details.put("path", request.getRequestURI());

        log.warn("Payload inválido em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        ApiResponse<Map<String, Object>> body = ApiResponse.error(details, "Corpo da requisição inválido ou malformado.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String rootMsg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        String lower = rootMsg != null ? rootMsg.toLowerCase() : "";

        Map<String, Object> details = new HashMap<>();
        details.put("error", "DataIntegrityViolation");
        details.put("path", request.getRequestURI());

        String clientMsg = "Operação violou uma restrição de dados (unicidade/foreign key/etc.).";

        // Heurística para violações de unicidade da matrícula (employeeId)
        if (lower.contains("employee_id") || lower.contains("teachers_employee_id") || lower.contains("unique") && lower.contains("employee")) {
            clientMsg = "Matrícula já cadastrada: 'employeeId' deve ser único.";
            details.put("field", "employeeId");
            details.put("reason", "unique_violation");
        }

        log.warn("Violação de integridade de dados em {}: {}", request.getRequestURI(), rootMsg);

        ApiResponse<Map<String, Object>> body = ApiResponse.error(details, clientMsg);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {

        log.warn("Estado inválido em {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("error", "IllegalState");
        details.put("path", request.getRequestURI());

        ApiResponse<Map<String, Object>> body = ApiResponse.error(details, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGlobalException(
            Exception ex, HttpServletRequest request) {

        log.error("Erro interno do servidor em {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> details = new HashMap<>();
        details.put("error", ex.getClass().getSimpleName());
        details.put("path", request.getRequestURI());
        details.put("details", ex.getMessage());

        ApiResponse<Map<String, Object>> body = ApiResponse.error(details, "Erro interno do servidor. Consulte os detalhes e tente novamente.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

