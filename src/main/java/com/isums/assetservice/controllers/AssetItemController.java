package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.domains.entities.AssetItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/asset/items")
@RequiredArgsConstructor
public class AssetItemController {
    private final AssetItemService assetItemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssetItemDto>>> GetAllAssetItems() {
        ApiResponse<List<AssetItemDto>> response = assetItemService.GetAllAssetItems();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssetItem>> CreateAssetItem(@RequestBody CreateAssetItemRequest request) {
        ApiResponse<AssetItem> response = assetItemService.CreateAssetItem(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetItemDto>> UpdateAssetItem(@PathVariable UUID id, @RequestBody UpdateAssetItemRequest request) {
        ApiResponse<AssetItemDto> response = assetItemService.UpdateAssetItem(id, request);
        return ResponseEntity.status(response.getStatusCode()).body(response);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> DeleteAssetItem(@PathVariable UUID id) {
        ApiResponse<Void> response = assetItemService.deleteAssetItem(id);
        return ResponseEntity.status(response.getStatusCode()).body(response);

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
}
