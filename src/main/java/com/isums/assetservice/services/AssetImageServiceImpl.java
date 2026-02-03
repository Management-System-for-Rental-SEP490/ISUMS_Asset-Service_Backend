package com.isums.assetservice.services;

import com.isums.assetservice.infrastructures.abstracts.AssetImageService;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.CreateAssetImageRequest;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AssetImageServiceImpl implements AssetImageService {
    private final AssetImageRepository assetImageRepository;
    private final AssetItemRepository assetItemRepository;
    private final AssetMapper assetMapper;

    @Override
    public AssetImageDto createImage(CreateAssetImageRequest request) {
        try{

            AssetItem assetItem = assetItemRepository
                    .findById(request.assetId())
                    .orElseThrow(() -> new RuntimeException("AssetItem not found"));

            if (request.imageUrl() == null || request.imageUrl().isBlank()) {
                throw new RuntimeException("imageUrl not found");
            }
            AssetImage assetImage = AssetImage.builder()
                    .assetItem(assetItem)
                    .imageUrl(request.imageUrl())
                    .note(request.note())
                    .createdAt(Instant.now())
                    .build();

            AssetImage created = assetImageRepository.save(assetImage);
            return assetMapper.mapAssetImage(assetImage);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());         }
        }

    @Override
    public List<AssetImageDto> getAllAssetImages() {
        try{
            List<AssetImage> mapAssetImages = assetImageRepository.findAll();
            return assetMapper.maAssetImages(mapAssetImages);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());         }
    }
}




