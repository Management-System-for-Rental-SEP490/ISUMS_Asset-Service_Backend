package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.IoTDeviceDto;

import java.util.UUID;

public interface IoTDeviceService {

    public IoTDeviceDto getByThing(String thing);
}
