package com.isums.assetservice.services;

import com.isums.assetservice.infrastructures.abstracts.AssetCategoryService;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.UpdateAssetCategoryRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetCategoryServiceImpl implements AssetCategoryService {
    private final AssetCategoryRepository categoryRepository;
    private final AssetMapper assetMapper;

    @Override
    public AssetCategoryDto createAssetCategory(CreateAssetCategoryRequest request) {
        try {
            AssetCategory assetCategory = AssetCategory.builder()
                    .name(request.name())
                    .compensationPercent(request.compensationPercent())
                    .description(request.description())
                    .build();

            AssetCategory created = categoryRepository.save(assetCategory);
            return assetMapper.mapAssetCategory(created);
        } catch (Exception ex) {
            throw new RuntimeException("Error to create asset categories: " + ex.getMessage());
        }
    }

    @Override
    public List<AssetCategoryDto> getAllAssetCategories() {
        try {
            List<AssetCategory> mapAssetCategories = categoryRepository.findAll();
            return assetMapper.mapAssetCategories(mapAssetCategories);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get all asset categories: " + ex.getMessage());        }
    }

    @Override
    public AssetCategoryDto updateAssetCategory(UUID id, UpdateAssetCategoryRequest request) {
        try {
            AssetCategory assetCategory = categoryRepository.findById(id)
                    .orElseThrow(()-> new RuntimeException("Id not found"));
            //Validation name
            if (request.name() != null && request.name().isBlank()) {
                throw new RuntimeException("Name isn't correct form");
            }

            //Validation CompensationPercent
            if (request.compensationPercent() != null) {
                if (request.compensationPercent() < 0 || request.compensationPercent() > 100) {
                    throw new RuntimeException("compensation must be in 0 - 100");
                }
            }

            if (request.name() != null) {
                assetCategory.setName(request.name());

            }
            if (request.compensationPercent() != null) {
                assetCategory.setCompensationPercent(request.compensationPercent());

            }
            if (request.description() != null) {
                assetCategory.setDescription(request.description());

            }

            AssetCategory updated = categoryRepository.save(assetCategory);
            return assetMapper.mapAssetCategory(assetCategory);


        } catch (Exception ex) {
            throw new RuntimeException("Error to update asset item: " + ex.getMessage());
        }
    }

    @Override
    public Boolean deleteAssetCategory(UUID id) {
        return null;
    }

}
