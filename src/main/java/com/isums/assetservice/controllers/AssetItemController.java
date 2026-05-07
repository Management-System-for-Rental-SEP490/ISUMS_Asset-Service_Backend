package com.isums.assetservice.controllers;

import com.isums.assetservice.domains.dtos.*;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateHouseRequest;
import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.CreateAssetItemRequest;
import com.isums.assetservice.domains.dtos.AssetItemDTO.UpdateAssetItemRequest;
import com.isums.assetservice.infrastructures.grpcs.GrpcUserClient;
import com.isums.observability.audit.AuditAction;
import com.isums.userservice.grpc.UserResponse;
import common.paginations.dtos.PageRequestParams;
import common.paginations.dtos.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
    @AuditAction(action = "ASSET.LIST", resourceType = "ASSET")
    public ApiResponse<PageResponse<AssetItemDto>> GetAll(@ParameterObject @ModelAttribute PageRequestParams params) {
        PageResponse<AssetItemDto> response = assetItemService.getAll(params.toPageRequest());
        return ApiResponses.ok(response, msg("asset.get_all"));
    }

    @PostMapping
    @AuditAction(action = "ASSET.CREATE", resourceType = "ASSET")
    @Operation(
            summary = "Tao metadata asset",
            description = """
                    Flow khuyen nghi khi tao asset tren Postman/Swagger:
                    1. Goi POST /api/assets/items de tao metadata asset.
                    2. Trong request create, field assetImages nen de [] neu anh dung API upload anh rieng.
                    3. Sau khi create thanh cong va lay duoc assetId, goi POST /api/assets/items/{assetId}/images voi multipart/form-data key = files de upload anh.

                    houseId + functionAreaId dung de gan asset vao dung nha / dung khu vuc.
                    categoryId la loai asset.
                    displayName la map da ngon ngu, toi thieu nen co khoa vi.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Create asset without images",
                            value = """
                                    {
                                      "houseId": "70279423-989d-48dc-8f2e-9bd6508a6f4a",
                                      "functionAreaId": "d7eb14d0-110e-4534-898b-d7d680cff898",
                                      "categoryId": "f1eaaa88-1e36-4454-b345-91cd6a06a6c1",
                                      "displayName": {
                                        "vi": "TV phong khach"
                                      },
                                      "serialNumber": "TV-SS-003",
                                      "conditionPercent": 80,
                                      "status": "IN_USE",
                                      "assetImages": []
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AssetItemDto> CreateAssetItem(@RequestBody CreateAssetItemRequest request) {
        AssetItemDto response = assetItemService.CreateAssetItem(request);
        return ApiResponses.ok(response, msg("asset.create"));
    }

    @PutMapping("/{id}")
    @AuditAction(action = "ASSET.UPDATE", resourceType = "ASSET", resourceIdExpression = "#args[0]")
    public ApiResponse<AssetItemDto> UpdateAssetItem(@PathVariable UUID id, @RequestBody UpdateAssetItemRequest request) {
        AssetItemDto response = assetItemService.UpdateAssetItem(id, request);
        return ApiResponses.ok(response, msg("asset.update"));
    }

    @DeleteMapping("/{id}")
    @AuditAction(action = "ASSET.DELETE", resourceType = "ASSET", resourceIdExpression = "#args[0]")
    public ApiResponse<Boolean> DeleteAssetItem(@PathVariable UUID id) {
        Boolean response = assetItemService.deleteAssetItem(id);
        return ApiResponses.ok(response, msg("asset.delete"));
    }

    @GetMapping("/{id}")
    @AuditAction(action = "ASSET.VIEW", resourceType = "ASSET", resourceIdExpression = "#args[0]")
    public ApiResponse<AssetItemDto> getAssetItemById(@PathVariable UUID id) {
        AssetItemDto res = assetItemService.getAssetItemById(id);
        return ApiResponses.ok(res, msg("asset.get_by_id"));
    }

    @GetMapping("/house/{id}")
    @AuditAction(action = "ASSET.LIST_BY_HOUSE", resourceType = "HOUSE", resourceIdExpression = "#args[0]")
    public ApiResponse<List<AssetItemDto>> getAssetItemByHouseId(@PathVariable UUID id) {
        return ApiResponses.ok(assetItemService.getAssetItemsByHouseId(id), msg("asset.get_by_house"));
    }

    @GetMapping("/house/{houseId}/function-area-counts")
    @AuditAction(action = "ASSET.COUNT_BY_HOUSE_AREA", resourceType = "HOUSE", resourceIdExpression = "#args[0]")
    public ApiResponse<List<AssetCountByFunctionAreaDto>> getAssetCountByFunctionArea(@PathVariable UUID houseId) {
        return ApiResponses.ok(assetItemService.getAssetCountByFunctionArea(houseId), msg("asset.count_by_house_area"));
    }

    @GetMapping("/house/{houseId}/function-area/{functionAreaId}")
    @AuditAction(action = "ASSET.LIST_BY_HOUSE_AREA", resourceType = "HOUSE", resourceIdExpression = "#args[0]")
    public ApiResponse<List<AssetItemDto>> getAssetItemByHouseIdAndFunctionAreaId(
            @PathVariable UUID houseId,
            @PathVariable UUID functionAreaId
    ) {
        return ApiResponses.ok(
                assetItemService.getAssetItemsByHouseIdAndFunctionAreaId(houseId, functionAreaId),
                msg("asset.get_by_house_area")
        );
    }

    @PutMapping("/{id}/transfer")
    @AuditAction(action = "ASSET.TRANSFER", resourceType = "ASSET", resourceIdExpression = "#args[0]")
    public ApiResponse<AssetItemDto> updateHouseForAsset(@PathVariable UUID id,
                                                         @RequestBody UpdateHouseRequest request,
                                                         @AuthenticationPrincipal Jwt jwt){
        UUID userId = UUID.fromString(jwt.getSubject());
        return ApiResponses.ok(assetItemService.updateHouseForAsset(id, request, userId), msg("asset.update_house"));
    }

    @PostMapping(value = "/{assetId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditAction(action = "ASSET.IMAGE_UPLOAD", resourceType = "ASSET", resourceIdExpression = "#args[0]")
    @Operation(
            summary = "Upload anh cho asset",
            description = """
                    API nay upload anh sau khi asset da duoc tao.
                    Postman dung multipart/form-data:
                    - key: files
                    - type: File
                    - co the chon nhieu file cung key files
                    """
    )
    public ApiResponse<?> uploadAssetImages(@PathVariable UUID assetId, @RequestParam("files") List<MultipartFile> files) {
        assetItemService.uploadAssetImages(assetId, files);
        return ApiResponses.ok(null, msg("asset.upload_images"));
    }

    @DeleteMapping("{assetId}/image/{imageId}")
    @AuditAction(action = "ASSET.IMAGE_DELETE", resourceType = "ASSET", resourceIdExpression = "#args[0]")
    public ApiResponse<Void> deleteAssetImage(@PathVariable UUID assetId, @PathVariable UUID imageId) {
        assetItemService.deleteAssetImage(assetId, imageId);
        return ApiResponses.ok(null, msg("asset.delete_image"));
    }

    @PutMapping("/maintenance/batch")
    @AuditAction(action = "ASSET.CONDITION_BATCH_UPDATE", resourceType = "ASSET")
    public ApiResponse<BatchUpdateResponse> batchUpdateAssetCondition(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody BatchUpdateAssetRequest request
    ) {
        UserResponse user = grpcUserClient.getUserIdAndRoleByKeyCloakId(jwt.getSubject());
        BatchUpdateResponse res = assetItemService.batchUpdateAssetCondition(UUID.fromString(user.getId()), request);
        return ApiResponses.ok(res, msg("asset.batch_update"));
    }

    @PutMapping("/{assetId}/manager-confirm-asset")
    @AuditAction(action = "ASSET.CONFIRM", resourceType = "ASSET", resourceIdExpression = "#args[0]")
    public ApiResponse<AssetItemDto> confirmAsset(@PathVariable UUID assetId, @RequestBody ConfirmAssetRequest request) {
        AssetItemDto res = assetItemService.confirmAsset(assetId, request.status());
        return ApiResponses.ok(res, msg("asset.confirm"));
    }
}
