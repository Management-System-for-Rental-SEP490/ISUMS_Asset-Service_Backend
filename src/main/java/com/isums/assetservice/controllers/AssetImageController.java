package com.isums.assetservice.controllers;


import com.isums.assetservice.abstracts.AssetImageService;
import com.isums.assetservice.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.CreateAssetImageRequest;
import com.isums.assetservice.domains.entities.AssetImage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asset-images")
@RequiredArgsConstructor
public class AssetImageController {
    private final AssetImageService assetImageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssetImageDto>>> getAllAssetImages(){
        ApiResponse<List<AssetImageDto>> response = assetImageService.getAllAssetImages();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssetImage>> createAssetImage(CreateAssetImageRequest request){
        ApiResponse<AssetImage> response = assetImageService.createImage(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
