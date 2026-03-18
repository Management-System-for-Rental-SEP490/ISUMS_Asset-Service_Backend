package com.isums.assetservice.infrastructures.mapper;

import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.dtos.IotControllerDto;
import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.entities.IotController;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IoTControllerMapper {

    @Mapping(target = "devices", ignore = true)
    IotControllerDto toIotControllerDto(IotController controller);
}
