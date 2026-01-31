package com.isums.assetservice.services;

import com.isums.assetservice.abstracts.AssetImageService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.CreateAssetImageRequest;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AssetImageServiceImpl implements AssetImageService {
    private final AssetImageQuery assetImageQuery;
    private final AssetItemRepository assetItemRepository;
    private final AssetMapper assetMapper;

    @Override
    public ApiResponse<AssetImageDto> createImage(CreateAssetImageRequest request) {
        try{

            AssetItem assetItem = assetItemRepository
                    .findById(request.assetId())
                    .orElseThrow(() -> new RuntimeException("AssetItem not found"));

            if (request.imageUrl() == null || request.imageUrl().isBlank()) {
                return ApiResponses.fail(HttpStatus.BAD_REQUEST, "ImageUrl required");
            }
            AssetImage assetImage = AssetImage.builder()
                    .assetItem(assetItem)
                    .imageUrl(request.imageUrl())
                    .note(request.note())
                    .createAt(Instant.now())
                    .build();

            AssetImage created = assetImageQuery.createAssetImage(assetImage);
            AssetImageDto assetImageDto = assetMapper.mapAssetImage((created));
            return ApiResponses.created(assetImageDto,"Create Image successfully");
        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to create item " + ex.getMessage());
        }
    }

    @Override
    public ApiResponse<List<AssetImageDto>> getAllAssetImages() {
        try{
            List<AssetImageDto> mapAssetImages = assetImageQuery.getAllAsset();
            return ApiResponses.ok(mapAssetImages,"Get all images successfully");
        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR,"fail to create item" + ex.getMessage());
        }
    }
}
