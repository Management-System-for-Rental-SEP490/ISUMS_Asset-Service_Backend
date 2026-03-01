package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.infrastructures.abstracts.AssetImageService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.CreateAssetImageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/asset/images")
@RequiredArgsConstructor
public class AssetImageController {
    private final AssetImageService assetImageService;

    @GetMapping
    public ApiResponse<List<AssetImageDto>> getAllAssetImages(){
        List<AssetImageDto> response = assetImageService.getAllAssetImages();
        return ApiResponses.ok(response,"Get all asset-images successfully");
    }

    @PostMapping
    public ApiResponse<AssetImageDto> createAssetImage(@RequestBody CreateAssetImageRequest request){
       AssetImageDto response = assetImageService.createImage(request);
        return ApiResponses.created(response,"Create asset-images successfully");
    }
}
