package com.isums.assetservice.domains.dtos;

import java.util.UUID;

public record ProvisionNodeRequest(String token, String serial, UUID areaId) {
}
