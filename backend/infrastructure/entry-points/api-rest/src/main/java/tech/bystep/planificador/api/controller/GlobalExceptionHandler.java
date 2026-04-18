package tech.bystep.planificador.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.bystep.planificador.api.dto.response.ApiResponse;
import tech.bystep.planificador.api.dto.response.ControlledErrorResponse;
import tech.bystep.planificador.api.dto.response.ErrorDetail;
import tech.bystep.planificador.api.dto.response.ErrorMeta;
import tech.bystep.planificador.security.UserPrincipal;
import tech.bystep.planificador.usecase.OrganizationUseCase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final OrganizationUseCase organizationUseCase;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(ApiResponse.<Map<String, String>>builder()
                .success(false).message("Validation failed").data(errors).build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ControlledErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(buildError("Recurso no encontrado",
                HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ControlledErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(buildError("Operación no permitida",
                HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ControlledErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildError("Acceso denegado",
                HttpStatus.FORBIDDEN.value(), "No tienes permisos para realizar esta acción"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ControlledErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildError(
                "Error inesperado", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocurrió un error interno. Por favor contacta al soporte."));
    }

    private ControlledErrorResponse buildError(String title, int code, String description) {
        UserPrincipal principal = extractPrincipal();
        String orgName = resolveOrgName(principal);
        String userId = principal != null ? principal.getUserId() : UUID.randomUUID().toString();

        return ControlledErrorResponse.builder()
                .meta(ErrorMeta.builder()
                        .messageId(userId)
                        .date(LocalDateTime.now().format(DATE_FMT))
                        .organization(orgName)
                        .build())
                .data(ControlledErrorResponse.ErrorData.builder()
                        .title(title)
                        .errors(List.of(ErrorDetail.builder()
                                .code(code)
                                .description(description)
                                .build()))
                        .build())
                .build();
    }

    private UserPrincipal extractPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        return null;
    }

    private String resolveOrgName(UserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null
                || principal.getOrganizationId().isBlank()) {
            return "Platform";
        }
        try {
            return organizationUseCase.findById(java.util.UUID.fromString(principal.getOrganizationId()))
                    .map(org -> org.getName())
                    .orElse(principal.getOrganizationId());
        } catch (Exception e) {
            return principal.getOrganizationId();
        }
    }
}
