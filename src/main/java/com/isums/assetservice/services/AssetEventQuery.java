package com.isums.assetservice.services;


import com.isums.assetservice.domains.dtos.AssetEventDTO.AssetEventDto;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetEventQuery {
    private final AssetEventRepository assetEventRepository;
    private final AssetMapper assetMapper;

    @CacheEvict(allEntries = true, cacheNames = "allAssetEvents")
    public AssetEvent createAsset(AssetEvent assetEvent){
        return assetEventRepository.save(assetEvent);
    }

    public AssetEvent save(AssetEvent assetEvent){
        return assetEventRepository.save(assetEvent);
    }

    @Cacheable(value = "allAssetEvents", sync = true)
    public List<AssetEventDto> getAllAsset(){
        List<AssetEvent> assetEvents = assetEventRepository.findAll();
        return assetMapper.maAssetEvents(assetEvents);
    }

    public AssetEvent findById(UUID id){
        return assetEventRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Access event not found"));
    }

    public void deleteById(UUID id){
        assetEventRepository.deleteById(id);
    }
}
