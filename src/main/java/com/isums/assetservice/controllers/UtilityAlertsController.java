package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.UtilityAlertsResponse;
import com.isums.assetservice.domains.enums.UtilityMetric;
import com.isums.assetservice.infrastructures.abstracts.UtilityAlertsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Utility alerts dashboard endpoint.
 *
 * <p><b>Path:</b> {@code GET /api/assets/utility-alerts}
 *
 * <p><b>Access:</b> LANDLORD sees their houses; MANAGER sees houses
 * in their regions; ADMIN treated as LANDLORD for now (cross-tenant
 * admin view TBD). TENANT and TECH_STAFF are rejected at the
 * {@code @PreAuthorize} gate — TENANT has a different single-house
 * endpoint; TECH_STAFF uses the job board, not the alerts fleet view.
 *
 * <p>The caller's identity comes from the JWT {@code sub} (Keycloak
 * id); the service-layer maps it to the internal user id + roles via
 * user-service gRPC. We don't trust the JWT roles claim for the scope
 * filter — they're only trusted for the coarse role gate here, and
 * the actual house ownership is resolved server-side from the DB.
 */
@RestController
@RequestMapping("/api/assets/utility-alerts")
@RequiredArgsConstructor
public class UtilityAlertsController {

    private final UtilityAlertsService utilityAlertsService;
    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LANDLORD','MANAGER','ADMIN')")
    public ApiResponse<UtilityAlertsResponse> listAlerts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "metric", defaultValue = "electricity") String metricParam
    ) {
        UtilityMetric metric = UtilityMetric.fromRequestParam(metricParam)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "metric must be one of: electricity, water"));

        UtilityAlertsResponse res = utilityAlertsService.listAlerts(jwt.getSubject(), metric);
        return ApiResponses.ok(res, msg("utility_alerts.retrieved"));
    }
}
