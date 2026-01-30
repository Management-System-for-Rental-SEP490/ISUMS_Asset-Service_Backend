package com.isums.assetservice.abstracts;

import com.google.protobuf.Api;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.CreateAssetImageRequest;
import com.isums.assetservice.domains.entities.AssetImage;

import java.util.List;

public interface AssetImageService {
    ApiResponse<AssetImage> createImage(CreateAssetImageRequest request);
    ApiResponse<List<AssetImageDto>> getAllAssetImages();

}
