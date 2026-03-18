package com.isums.assetservice.infrastructures.mapper;

import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.dtos.IoTDeviceMapControllerDto;
import com.isums.assetservice.domains.entities.IoTDevice;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface IoTDeviceMapper {

    IoTDeviceDto toIoTDeviceDto(IoTDevice iotDevice);

    List<IoTDeviceDto> toIoTDeviceDtoList(List<IoTDevice> iotDevices);


    @Mapping(source = "assetItem.id", target = "assetId")
    @Mapping(source = "assetItem.displayName", target = "displayName")
    @Mapping(source = "assetItem.category.code", target = "categoryCode")
    @Mapping(source = "assetItem.status", target = "status")
    @Mapping(target = "areaName", ignore = true)
    IoTDeviceMapControllerDto toIoTDeviceMapControllerDto(IoTDevice iotDevice, @Context Map<String, String> areaNameMap);

    List<IoTDeviceMapControllerDto> toIoTDeviceMapControllerDtoList(List<IoTDevice> iotDevices, @Context Map<String, String> areaNameMap);

    @AfterMapping
    default void setAreaName(IoTDevice device, @MappingTarget IoTDeviceMapControllerDto.IoTDeviceMapControllerDtoBuilder dto,
                             @Context Map<String, String> areaNameMap) {
        if (device.getAssetItem() != null && device.getAssetItem().getFunctionAreaId() != null) {
            String areaName = areaNameMap.get(device.getAssetItem().getFunctionAreaId().toString());
            dto.areaName(areaName);
        }
    }
}
