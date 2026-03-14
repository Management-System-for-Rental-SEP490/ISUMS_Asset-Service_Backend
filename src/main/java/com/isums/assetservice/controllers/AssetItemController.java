package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateHouseRequest;
import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/items")
@RequiredArgsConstructor
public class AssetItemController {
    private final AssetItemService assetItemService;

    @GetMapping
    public ApiResponse<List<AssetItemDto>> GetAllAssetItems() {
        List<AssetItemDto> response = assetItemService.GetAllAssetItems();
        return ApiResponses.ok(response,"Get all asset-items successfully");
    }

    @PostMapping
    public ApiResponse<AssetItemDto> CreateAssetItem(@RequestBody CreateAssetItemRequest request) {
        AssetItemDto response = assetItemService.CreateAssetItem(request);
        return ApiResponses.ok(response,"Create asset-item successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetItemDto> UpdateAssetItem(@PathVariable UUID id, @RequestBody UpdateAssetItemRequest request) {
        AssetItemDto response = assetItemService.UpdateAssetItem(id, request);
        return ApiResponses.ok(response,"Get all asset-images successfully");

    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> DeleteAssetItem(@PathVariable UUID id) {
        Boolean response = assetItemService.deleteAssetItem(id);
        return ApiResponses.ok(response,"Get all asset-images successfully");

    }

    @GetMapping("/{id}")
    public ApiResponse<AssetItemDto> getAssetItemById(@PathVariable UUID id) {
        AssetItemDto res = assetItemService.getAssetItemById(id);
        return ApiResponses.ok(res, "Success to get asset item");
    }

    @GetMapping("/house/{id}")
    public ApiResponse<List<AssetItemDto>> getAssetItemByHouseId(@PathVariable UUID id) {
        return ApiResponses.ok(assetItemService.getAssetItemsByHouseId(id), "Success to get asset item by house id");
    }

    @PutMapping("/{id}/transfer")
    public ApiResponse<AssetItemDto> updateHouseForAsset(@PathVariable UUID id,
                                                         @RequestBody UpdateHouseRequest request,
                                                         @AuthenticationPrincipal Jwt jwt){
        UUID userId = UUID.fromString(jwt.getSubject());
        return ApiResponses.ok(assetItemService.updateHouseForAsset(id,request,userId),"Update new house for asset-item successfully");
    }
}
