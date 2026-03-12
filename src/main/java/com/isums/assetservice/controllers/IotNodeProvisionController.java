package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.IotNodeTokenRequest;
import com.isums.assetservice.domains.dtos.IotNodeTokenResponse;
import com.isums.assetservice.infrastructures.abstracts.IotNodeTokenService;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/provision-token")
    public ApiResponse<IotNodeTokenResponse> getProvisionToken(@RequestBody IotNodeTokenRequest req) {

        String token = iotNodeTokenService.generateToken(req.serial());
        long expiresAt = Instant.now().plusSeconds(600).toEpochMilli();

        return ApiResponses.ok(new IotNodeTokenResponse(token, expiresAt), "Token generated, valid for 10 minutes");
    }
}
