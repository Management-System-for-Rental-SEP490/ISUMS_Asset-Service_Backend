package com.isums.assetservice.infrastructures.mapper;

import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.dtos.IotControllerDto;
import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.entities.IotController;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IoTControllerMapper {

    @Mapping(target = "devices",   ignore = true)
    @Mapping(target = "houseName", ignore = true)
    @Mapping(target = "areaName",  ignore = true)
    IotControllerDto toIotControllerDto(IotController controller);
}
