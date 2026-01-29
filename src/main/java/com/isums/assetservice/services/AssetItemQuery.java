package com.isums.assetservice.services;

import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

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

}
