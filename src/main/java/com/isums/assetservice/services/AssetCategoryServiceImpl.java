package com.isums.assetservice.services;

import com.isums.assetservice.infrastructures.abstracts.AssetCategoryService;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.CreateAssetCategoryRequest;
import com.isums.assetservice.domains.dtos.AssetCategoryDTO.UpdateAssetCategoryRequest;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import common.i18n.TranslationMap;
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
    private final TranslationAutoFillService translationAutoFillService;

    @Override
    public AssetCategoryDto createAssetCategory(CreateAssetCategoryRequest request) {
        try {
            AssetCategory assetCategory = AssetCategory.builder()
                    .name(translationAutoFillService.complete(request.name()))
                    .compensationPercent(request.compensationPercent())
                    .description(translationAutoFillService.complete(request.description()))
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
    public AssetCategoryDto getById(UUID id) {
        try{
            AssetCategory categories = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Id not found"));

            return assetMapper.mapAssetCategory(categories);

        } catch (Exception ex) {
            throw new RuntimeException("Can't get categories by id" + ex.getMessage());
        }
    }

    @Override
    public AssetCategoryDto updateAssetCategory(UUID id, UpdateAssetCategoryRequest request) {
        try {
            AssetCategory assetCategory = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Id not found"));

            // Validate: non-null name map must not be empty
            if (request.name() != null && request.name().isEmpty()) {
                throw new RuntimeException("Name isn't correct form");
            }

            // Validate compensationPercent range
            if (request.compensationPercent() != null) {
                if (request.compensationPercent() < 0 || request.compensationPercent() > 100) {
                    throw new RuntimeException("compensation must be in 0 - 100");
                }
            }

            if (request.name() != null) {
                assetCategory.setName(translationAutoFillService.complete(request.name()));
            }
            if (request.compensationPercent() != null) {
                assetCategory.setCompensationPercent(request.compensationPercent());
            }
            if (request.description() != null) {
                assetCategory.setDescription(translationAutoFillService.complete(request.description()));
            }

            categoryRepository.save(assetCategory);
            return assetMapper.mapAssetCategory(assetCategory);

        } catch (Exception ex) {
            throw new RuntimeException("Error to update asset category: " + ex.getMessage());
        }
    }

    @Override
    public Boolean deleteAssetCategory(UUID id) {
        return null;
    }

}
