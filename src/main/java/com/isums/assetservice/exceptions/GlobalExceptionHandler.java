package com.isums.assetservice.exceptions;

import com.isums.assetservice.domains.dtos.ApiError;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    private String msgOrRaw(String raw) {
        try {
            return messageSource.getMessage(raw, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return raw;
        }
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDb(DataAccessException ex) {
        String detail = ex.getMostSpecificCause().getMessage();

        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                msg("error.database"),
                List.of(ApiError.builder()
                        .code("DB_ERROR")
                        .message(detail)
                        .build())
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.BAD_REQUEST,
                msgOrRaw(ex.getMessage()),
                List.of(ApiError.builder()
                        .code("BAD_REQUEST")
                        .message(msgOrRaw(ex.getMessage()))
                        .build())
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled asset-service exception", ex);
        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                msg("error.unexpected"),
                List.of(ApiError.builder()
                        .code("INTERNAL_ERROR")
                        .message(msg("error.unexpected"))
                        .build())
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = msg("error.data_integrity");
        if (ex.getMessage() != null && ex.getMessage().contains("serial_number")) {
            message = msg("error.serial_exists");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponses.fail(HttpStatus.CONFLICT, message));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleConflictException(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponses.fail(HttpStatus.CONFLICT, msgOrRaw(ex.getMessage())));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.NOT_FOUND,
                msgOrRaw(ex.getMessage()),
                List.of(ApiError.builder()
                        .code("NOT_FOUND")
                        .message(msgOrRaw(ex.getMessage()))
                        .build())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();
        String reason = ex.getReason() != null ? msgOrRaw(ex.getReason()) : ex.getMessage();

        String errorCode = statusCode.value() == 402 ? "PAYMENT_REQUIRED"
                : statusCode.value() == 503       ? "SERVICE_UNAVAILABLE"
                : statusCode.value() == 502       ? "BAD_GATEWAY"
                : "HTTP_" + statusCode.value();

        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.resolve(statusCode.value()) != null
                        ? Objects.requireNonNull(HttpStatus.resolve(statusCode.value()))
                        : HttpStatus.INTERNAL_SERVER_ERROR,
                reason,
                List.of(ApiError.builder()
                        .code(errorCode)
                        .message(reason)
                        .build())
        );
        return ResponseEntity.status(statusCode).body(res);
    }
}
