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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private final MessageSource messageSource;

    private String msg(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    @GetMapping
    public ApiResponse<PageResponse<AssetItemDto>> GetAll(@ParameterObject @Valid @ModelAttribute PageRequestParams params) {
        PageResponse<AssetItemDto> response = assetItemService.getAll(params.toPageRequest());
        return ApiResponses.ok(response, msg("asset.get_all"));
    }

    @PostMapping
    public ApiResponse<AssetItemDto> CreateAssetItem(@RequestBody CreateAssetItemRequest request) {
        AssetItemDto response = assetItemService.CreateAssetItem(request);
        return ApiResponses.ok(response, msg("asset.create"));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetItemDto> UpdateAssetItem(@PathVariable UUID id, @RequestBody UpdateAssetItemRequest request) {
        AssetItemDto response = assetItemService.UpdateAssetItem(id, request);
        return ApiResponses.ok(response, msg("asset.update"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> DeleteAssetItem(@PathVariable UUID id) {
        Boolean response = assetItemService.deleteAssetItem(id);
        return ApiResponses.ok(response, msg("asset.delete"));
    }

    @GetMapping("/{id}")
    public ApiResponse<AssetItemDto> getAssetItemById(@PathVariable UUID id) {
        AssetItemDto res = assetItemService.getAssetItemById(id);
        return ApiResponses.ok(res, msg("asset.get_by_id"));
    }

    @GetMapping("/house/{id}")
    public ApiResponse<List<AssetItemDto>> getAssetItemByHouseId(@PathVariable UUID id) {
        return ApiResponses.ok(assetItemService.getAssetItemsByHouseId(id), msg("asset.get_by_house"));
    }

    @PutMapping("/{id}/transfer")
    public ApiResponse<AssetItemDto> updateHouseForAsset(@PathVariable UUID id,
                                                         @RequestBody UpdateHouseRequest request,
                                                         @AuthenticationPrincipal Jwt jwt){
        UUID userId = UUID.fromString(jwt.getSubject());
        return ApiResponses.ok(assetItemService.updateHouseForAsset(id, request, userId), msg("asset.update_house"));
    }

    @PostMapping(value = "/{assetId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> uploadAssetImages(@PathVariable UUID assetId, @RequestParam("files") List<MultipartFile> files) {
        assetItemService.uploadAssetImages(assetId, files);
        return ApiResponses.ok(null, msg("asset.upload_images"));
    }

    @DeleteMapping("{assetId}/image/{imageId}")
    public ApiResponse<Void> deleteAssetImage(@PathVariable UUID assetId, @PathVariable UUID imageId) {
        assetItemService.deleteAssetImage(assetId, imageId);
        return ApiResponses.ok(null, msg("asset.delete_image"));
    }

    @PutMapping("/maintenance/batch")
    public ApiResponse<BatchUpdateResponse> batchUpdateAssetCondition(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody BatchUpdateAssetRequest request
    ) {
        UserResponse user = grpcUserClient.getUserIdAndRoleByKeyCloakId(jwt.getSubject());
        BatchUpdateResponse res = assetItemService.batchUpdateAssetCondition(UUID.fromString(user.getId()), request);
        return ApiResponses.ok(res, msg("asset.batch_update"));
    }

    @PutMapping("/{assetId}/manager-confirm-asset")
    public ApiResponse<AssetItemDto> confirmAsset(@PathVariable UUID assetId, @RequestBody ConfirmAssetRequest request) {
        AssetItemDto res = assetItemService.confirmAsset(assetId, request.status());
        return ApiResponses.ok(res, msg("asset.confirm"));
    }
}
