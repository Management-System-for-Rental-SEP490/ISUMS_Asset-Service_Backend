package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.*;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateHouseRequest;
import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.infrastructures.grpcs.GrpcUserClient;
import com.isums.userservice.grpc.UserResponse;
import common.paginations.dtos.PageRequestParams;
import common.paginations.dtos.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/items")
@RequiredArgsConstructor
public class AssetItemController {
    private final AssetItemService assetItemService;
    private final GrpcUserClient grpcUserClient;

    @GetMapping
    public ApiResponse<PageResponse<AssetItemDto>> GetAll(@ParameterObject @Valid @ModelAttribute PageRequestParams params) {
        PageResponse<AssetItemDto> response = assetItemService.getAll(params.toPageRequest());
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
        return ApiResponses.ok(response,"Update asset item successfully");

    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> DeleteAssetItem(@PathVariable UUID id) {
        Boolean response = assetItemService.deleteAssetItem(id);
        return ApiResponses.ok(response,"Delete  asset items successfully");

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

    @PostMapping(value = "/{assetId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AssetItemDto> uploadAssetImages(@PathVariable UUID assetId, @RequestParam("files") List<MultipartFile> files) {
        assetItemService.uploadAssetImages(assetId, files);
        return ApiResponses.ok(null, "Upload images successfully");
    }

    @DeleteMapping("{assetId}/image/{imageId}")
    public ApiResponse<Void> deleteAssetImage(@PathVariable UUID assetId, @PathVariable UUID imageId) {
        assetItemService.deleteAssetImage(assetId, imageId);
        return ApiResponses.ok(null, "Delete image successfully");
    }

    @PutMapping("/maintenance/batch",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BatchUpdateResponse> batchUpdateAssetCondition(@AuthenticationPrincipal Jwt jwt,@RequestBody BatchUpdateAssetRequest request,
                                                                      @RequestPart(required = false) Map<String, List<MultipartFile>> files) {
        UserResponse user = grpcUserClient.getUserIdAndRoleByKeyCloakId(jwt.getSubject());
        BatchUpdateResponse res = assetItemService.batchUpdateWithImages(UUID.fromString(user.getId()),request,files);
        return ApiResponses.ok(res, "Batch update asset successfully");
    }

    @PutMapping("/{assetId}/manager-confirm-asset")
    public ApiResponse<AssetItemDto> confirmAsset(@PathVariable UUID assetId, @RequestBody ConfirmAssetRequest request) {
        AssetItemDto res = assetItemService.confirmAsset(assetId,request.status());
        return ApiResponses.ok(res, "Confirm asset successfully");
    }
}
