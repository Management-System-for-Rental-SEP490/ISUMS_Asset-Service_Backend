package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetCategoryDTO.AssetCategoryDto;
import com.isums.assetservice.domains.dtos.AssetImageDTO.AssetImageDto;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetImageQuery {
    private final AssetImageRepository assetImageRepository;
    private final AssetMapper assetMapper;

    @CacheEvict(allEntries = true, cacheNames = "allAssetImages")
    public AssetImage createAssetImage(AssetImage assetImage){
        return assetImageRepository.save(assetImage);
    }

    public AssetImage save(AssetImage image){
        return assetImageRepository.save(image);
    }

    @Cacheable(value = "allAssetImages", sync = true)
    public List<AssetImageDto> getAllAsset(){
        List<AssetImage> assetImages = assetImageRepository.findAll();
        return assetMapper.maAssetImages(assetImages);
    }

    public AssetImage findById(UUID id){
        return assetImageRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Access category not found"));
    }


    public void deleteById(UUID id){
        assetImageRepository.deleteById(id);
    }


}