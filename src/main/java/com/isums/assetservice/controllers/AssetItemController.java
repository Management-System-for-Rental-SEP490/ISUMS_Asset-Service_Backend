package com.isums.assetservice.controllers;


import com.isums.assetservice.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.entities.AssetItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-items")
@RequiredArgsConstructor
public class AssetItemController {
    private final AssetItemService assetItemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssetItem>>> GetAllAssetItems(){
        ApiResponse<List<AssetItem>> response = assetItemService.GetAllAssetItems();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssetItem>> CreateAssetItem(@RequestBody CreateAssetItemRequest request){
        ApiResponse<AssetItem> response = assetItemService.CreateAssetItem(request);
        return  ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
