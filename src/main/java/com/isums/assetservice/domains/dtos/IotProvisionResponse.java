package com.isums.assetservice.domains.dtos;

public record IotProvisionResponse(
        String thingName,
        String certificatePem,
        String privateKey,
        String mqttEndpoint,
        String houseId
) {
}
