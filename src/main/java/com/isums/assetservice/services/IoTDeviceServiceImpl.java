package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.infrastructures.abstracts.IoTDeviceService;
import com.isums.assetservice.infrastructures.mapper.IoTDeviceMapper;
import com.isums.assetservice.infrastructures.repositories.IoTDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IoTDeviceServiceImpl implements IoTDeviceService {

    private final IoTDeviceRepository iotDeviceRepository;
    private final IoTDeviceMapper iotDeviceMapper;

    @Override
    public IoTDeviceDto getByThing(String thing) {

        var iot = iotDeviceRepository.findViewByThing(thing)
                .orElseThrow(() -> new RuntimeException("IoT device not found for thing=" + thing));

        return new IoTDeviceDto(
                iot.getIotDeviceId(),
                iot.getThing(),
                iot.getSerialNumber(),
                iot.getAssetId(),
                iot.getHouseId(),
                iot.getCategoryId(),
                iot.getCategoryCode(),
                iot.getDetectionType()
        );
    }
}
