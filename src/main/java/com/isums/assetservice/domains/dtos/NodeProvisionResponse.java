package com.isums.assetservice.domains.dtos;

public record NodeProvisionResponse(
        String serial,
        String ctrlMac,
        String houseId,
        String assetId
) {}
