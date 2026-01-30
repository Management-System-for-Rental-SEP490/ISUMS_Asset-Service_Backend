package com.isums.assetservice.services;

import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetItemQuery {
    private final AssetItemRepository assetItemRepository;

    @CacheEvict(allEntries = true, cacheNames = "allAssetItems")
    public AssetItem createAssetItem(AssetItem assetItem){
        return assetItemRepository.save(assetItem);
    }

    @Cacheable(value = "allAssetItems", sync = true)
    public List<AssetItem> GetAllAssetItems(){
        return assetItemRepository.findAll();
    }

    public AssetItem findById(UUID id){
        return assetItemRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Access category not found"));
    }

    public void deleteById(UUID id){
        assetItemRepository.deleteById(id);
    }

}
