package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.CreateIoTDeviceRequest;
import com.isums.assetservice.domains.dtos.IoTDeviceDto;
import com.isums.assetservice.domains.entities.IoTDevice;

public interface IoTDeviceService {

    public IoTDeviceDto getByThing(String thing);

    public void upsetToDynamoDB(IoTDevice device);

    public void createIoTDevice(CreateIoTDeviceRequest request);
}
