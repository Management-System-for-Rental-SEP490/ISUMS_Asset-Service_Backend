package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetItemQuery {
    private final AssetItemRepository assetItemRepository;
    private final AssetMapper assetMapper;

    @CacheEvict(allEntries = true, cacheNames = "allAssetItems")
    public AssetItem createAssetItem(AssetItem assetItem){
        return assetItemRepository.save(assetItem);
    }

    //@Cacheable(value = "allAssetItems", sync = true)
    public List<AssetItemDto> GetAllAssetItems(){
        List<AssetItem> assetItems = assetItemRepository.findAll();
        return assetMapper.mapAssetItems(assetItems);
    }

    public AssetItem findById(UUID id){
        return assetItemRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Access item not found"));
    }

    public void deleteById(UUID id){
        assetItemRepository.deleteById(id);
    }

}
