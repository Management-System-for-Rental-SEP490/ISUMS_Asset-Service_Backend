package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetCategoryQuery {
    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetMapper assetMapper;

    @CacheEvict(allEntries = true, cacheNames = "allAssetCategories")
    public AssetCategory createAssetCategory(AssetCategory assetCategory){
        return assetCategoryRepository.save(assetCategory);
    }
    
    public AssetCategory save(AssetCategory category){
        return assetCategoryRepository.save(category);
    }

    @Cacheable(value = "allAssetCategories", sync = true)
    public List<AssetCategoryDto> getAllAsset(){
        List<AssetCategory> assetCategories = assetCategoryRepository.findAll();
        return assetMapper.mapAssetCategories(assetCategories);
    }

    public AssetCategory findById(UUID id){
        return assetCategoryRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Access category not found"));
    }


    public void deleteById(UUID id){
        assetCategoryRepository.deleteById(id);
    }


}
