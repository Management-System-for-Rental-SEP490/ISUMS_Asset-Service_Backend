package com.isums.assetservice.controllers;


import com.isums.assetservice.domains.dtos.ApiResponses;
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
@RequestMapping("api/assets/categories")
@RequiredArgsConstructor
public class AssetCategoryController {
    private final AssetCategoryService assetCategoryService;

    @GetMapping
    public ApiResponse<List<AssetCategoryDto>> GetAllAssetCategories(){
        List<AssetCategoryDto> response = assetCategoryService.getAllAssetCategories();
        return ApiResponses.ok(response,"Get all categories successfully");
    }

    @PostMapping
    public ApiResponse<AssetCategoryDto> CreateAssetCategory(@RequestBody CreateAssetCategoryRequest request){
        AssetCategoryDto response = assetCategoryService.createAssetCategory(request);
        return ApiResponses.created(response,"Create category successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetCategoryDto> updateAssetCategory(@PathVariable UUID id, @RequestBody UpdateAssetCategoryRequest request){
        AssetCategoryDto response = assetCategoryService.updateAssetCategory(id,request);
        return ApiResponses.ok(response,"Get all categories successfully");
    }
}
