package com.isums.assetservice.controllers;


import com.isums.assetservice.infrastructures.abstracts.AssetCategoryService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.UpdateAssetCategoryRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/categories")
@RequiredArgsConstructor
public class AssetCategoryController {
    private final AssetCategoryService assetCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssetCategoryDto>>> GetAllAssetCategories(){
        ApiResponse<List<AssetCategoryDto>> response = assetCategoryService.getAllAssetCategories();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssetCategory>> CreateAssetCategory(@RequestBody CreateAssetCategoryRequest request){
        ApiResponse<AssetCategory> response = assetCategoryService.createAssetCategory(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetCategoryDto>> updateAssetCategory(@PathVariable UUID id, @RequestBody UpdateAssetCategoryRequest request){
        ApiResponse<AssetCategoryDto> response = assetCategoryService.updateAssetCategory(id,request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
