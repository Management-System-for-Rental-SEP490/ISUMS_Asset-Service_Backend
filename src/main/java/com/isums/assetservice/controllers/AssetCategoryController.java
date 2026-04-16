package com.isums.assetservice.controllers;


import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.infrastructures.abstracts.AssetCategoryService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.UpdateAssetCategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/categories")
@RequiredArgsConstructor
public class AssetCategoryController {
    private final AssetCategoryService assetCategoryService;
    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    @GetMapping
    public ApiResponse<List<AssetCategoryDto>> GetAllAssetCategories(){
        List<AssetCategoryDto> response = assetCategoryService.getAllAssetCategories();
        return ApiResponses.ok(response, msg("category.get_all"));
    }

    @GetMapping("/{id}")
    public ApiResponse<AssetCategoryDto> GetAssetCategoryById(@PathVariable UUID id){
        AssetCategoryDto response = assetCategoryService.getById(id);
        return ApiResponses.ok(response, msg("category.get_by_id"));
    }

    @PostMapping
    public ApiResponse<AssetCategoryDto> CreateAssetCategory(@RequestBody CreateAssetCategoryRequest request){
        AssetCategoryDto response = assetCategoryService.createAssetCategory(request);
        return ApiResponses.created(response, msg("category.create"));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetCategoryDto> updateAssetCategory(@PathVariable UUID id, @RequestBody UpdateAssetCategoryRequest request){
        AssetCategoryDto response = assetCategoryService.updateAssetCategory(id, request);
        return ApiResponses.ok(response, msg("category.get_all"));
    }
}
