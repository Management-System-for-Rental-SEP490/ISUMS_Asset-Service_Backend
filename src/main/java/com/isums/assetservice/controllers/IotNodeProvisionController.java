package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.IotNodeTokenRequest;
import com.isums.assetservice.domains.dtos.IotNodeTokenResponse;
import com.isums.assetservice.infrastructures.abstracts.IotNodeTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/assets/iot")
@RequiredArgsConstructor
public class IotNodeProvisionController {

    private final IotNodeTokenService iotNodeTokenService;
    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    @PostMapping("/provision-token")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ApiResponse<IotNodeTokenResponse> getProvisionToken(@RequestBody IotNodeTokenRequest req) {
        String token = iotNodeTokenService.generateToken(req.serial());
        long expiresAt = Instant.now().plusSeconds(600).toEpochMilli();
        return ApiResponses.ok(new IotNodeTokenResponse(token, expiresAt), msg("iot.token_generated"));
    }
}
