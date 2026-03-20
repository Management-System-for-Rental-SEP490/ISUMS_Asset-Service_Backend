package com.isums.assetservice.infrastructures.mapper;

import com.isums.assetservice.domains.dtos.ThresholdResponse;
import com.isums.assetservice.domains.entities.IotThreshold;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IotThresholdMapper {
    ThresholdResponse toResponse(IotThreshold threshold);
    List<ThresholdResponse> toResponseList(List<IotThreshold> thresholds);
}
