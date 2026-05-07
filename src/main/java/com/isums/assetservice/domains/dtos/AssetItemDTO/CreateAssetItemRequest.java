package com.isums.assetservice.domains.dtos.AssetItemDTO;

import com.isums.assetservice.domains.enums.AssetStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code displayName} is a multilingual map, e.g. {@code {"vi":"Ban","en":"Table"}}.
 * Supply at least one entry; "vi" is the default fallback.
 */
public record CreateAssetItemRequest(
        @Schema(description = "ID nha chua asset", example = "70279423-989d-48dc-8f2e-9bd6508a6f4a")
        UUID houseId,
        @Schema(description = "ID khu vuc trong nha chua asset", example = "d7eb14d0-110e-4534-898b-d7d680cff898")
        UUID functionAreaId,
        @Schema(description = "ID category cua asset", example = "f1eaaa88-1e36-4454-b345-91cd6a06a6c1")
        UUID categoryId,
        @Schema(
                description = "Ten asset dang map da ngon ngu. Toi thieu nen co khoa vi. Backend se auto-fill them translation neu thieu.",
                example = "{\"vi\":\"TV phong khach\"}"
        )
        Map<String, String> displayName,
        @Schema(description = "Serial number hoac ma quan ly noi bo cua asset", example = "TV-SS-003")
        String serialNumber,
        @Schema(description = "Tinh trang phan tram ban dau cua asset", example = "80")
        int conditionPercent,
        @Schema(description = "Trang thai asset khi tao", example = "IN_USE")
        AssetStatus status,
        @Schema(
                description = "Danh sach string anh/metadata cu. Voi flow Postman hien tai nen de [] va upload anh qua API /api/assets/items/{assetId}/images sau khi create.",
                example = "[]"
        )
        List<String> assetImages
) {
}
