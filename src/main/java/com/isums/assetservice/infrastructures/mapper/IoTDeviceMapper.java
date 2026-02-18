package com.isums.assetservice.infrastructures.mapper;

import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.entities.IoTDevice;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IoTDeviceMapper {

    IoTDeviceDto toIoTDeviceDto(IoTDevice iotDevice);
}
