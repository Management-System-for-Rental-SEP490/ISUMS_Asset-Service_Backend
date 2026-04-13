package com.isums.assetservice.exceptions;

import com.isums.assetservice.domains.dtos.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler (asset-service)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleDb returns 500 with DB_ERROR code and root cause message")
    void db() {
        DataAccessException ex = new DataAccessException("outer", new RuntimeException("root")) {};

        ResponseEntity<ApiResponse<Void>> res = handler.handleDb(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(500);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("DB_ERROR");
        assertThat(res.getBody().getErrors().get(0).getMessage()).isEqualTo("root");
    }

    @Test
    @DisplayName("handleBadRequest returns 400 with BAD_REQUEST code")
    void badRequest() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleBadRequest(new IllegalArgumentException("nope"));
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("handleGeneric returns 500 with INTERNAL_ERROR code")
    void generic() {
        ResponseEntity<ApiResponse<Void>> res = handler.handleGeneric(new Exception("boom"));
        assertThat(res.getStatusCode().value()).isEqualTo(500);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("handleDataIntegrityViolation returns 409 with serial_number-specific message")
    void dataIntegritySerial() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("duplicate key violates uk_serial_number");

        ResponseEntity<ApiResponse<?>> res = handler.handleDataIntegrityViolation(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().getMessage()).isEqualTo("Serial number already exists");
    }

    @Test
    @DisplayName("handleDataIntegrityViolation returns 409 with generic message otherwise")
    void dataIntegrityGeneric() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("fk violation");
        ResponseEntity<ApiResponse<?>> res = handler.handleDataIntegrityViolation(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().getMessage()).isEqualTo("Data integrity violation");
    }

    @Test
    @DisplayName("handleConflictException returns 409")
    void conflict() {
        ResponseEntity<ApiResponse<?>> res = handler.handleConflictException(new ConflictException("dup"));
        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().getMessage()).isEqualTo("dup");
    }

    @Test
    @DisplayName("handleNotFound returns 404 with NOT_FOUND code")
    void notFound() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleNotFound(new NotFoundException("missing"));
        assertThat(res.getStatusCode().value()).isEqualTo(404);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("handleResponseStatus 402 → PAYMENT_REQUIRED code")
    void rs402() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "pay");

        ResponseEntity<ApiResponse<Void>> res = handler.handleResponseStatus(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(402);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("PAYMENT_REQUIRED");
    }

    @Test
    @DisplayName("handleResponseStatus 503 → SERVICE_UNAVAILABLE code")
    void rs503() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "down");

        ResponseEntity<ApiResponse<Void>> res = handler.handleResponseStatus(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(503);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("handleResponseStatus 502 → BAD_GATEWAY code")
    void rs502() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_GATEWAY, "gw");

        ResponseEntity<ApiResponse<Void>> res = handler.handleResponseStatus(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(502);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("BAD_GATEWAY");
    }

    @Test
    @DisplayName("handleResponseStatus other status → HTTP_<code>")
    void rsOther() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "teapot");

        ResponseEntity<ApiResponse<Void>> res = handler.handleResponseStatus(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(418);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("HTTP_418");
    }
}
