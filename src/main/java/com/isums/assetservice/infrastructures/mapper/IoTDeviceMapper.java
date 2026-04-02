package com.isums.assetservice.infrastructures.mapper;

import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.dtos.IoTDeviceMapControllerDto;
import com.isums.assetservice.domains.entities.IoTDevice;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface IoTDeviceMapper {

    IoTDeviceDto toIoTDeviceDto(IoTDevice iotDevice);

    List<IoTDeviceDto> toIoTDeviceDtoList(List<IoTDevice> iotDevices);

    @Mapping(source = "assetItem.id", target = "assetId")
    @Mapping(source = "assetItem.displayName", target = "displayName")
    @Mapping(source = "assetItem.category.code", target = "categoryCode")
    @Mapping(source = "assetItem.status", target = "status")
    @Mapping(source = "assetItem.functionAreaId", target = "areaName", qualifiedByName = "mapAreaName")
    IoTDeviceMapControllerDto toIoTDeviceMapControllerDto(
            IoTDevice iotDevice,
            @Context Map<String, String> areaNameMap
    );

    List<IoTDeviceMapControllerDto> toIoTDeviceMapControllerDtoList(
            List<IoTDevice> iotDevices,
            @Context Map<String, String> areaNameMap
    );

    @Named("mapAreaName")
    default String mapAreaName(UUID functionAreaId, @Context Map<String, String> areaNameMap) {
        if (functionAreaId == null) return null;
        return areaNameMap.get(functionAreaId.toString());
    }
}
