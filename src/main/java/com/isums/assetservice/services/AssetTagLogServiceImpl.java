package com.isums.assetservice.services;

import com.isums.assetservice.domains.entities.AssetTagLog;
import com.isums.assetservice.infrastructures.abstracts.AssetTagLogService;
import com.isums.assetservice.infrastructures.repositories.AssetTagLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetTagLogServiceImpl implements AssetTagLogService{
    private final AssetTagLogRepository assetTagLogRepository;
    @Override
    public List<AssetTagLog> getLogsByTag(String tagValue) {
        try{
            if(tagValue == null || tagValue.isBlank()){
                throw new IllegalArgumentException("Tag Value must not be empty");
            }

            return assetTagLogRepository
                    .findByTagValueOrderByCreatedAtDesc(tagValue);

        } catch (Exception ex) {
            throw new RuntimeException("Error to get logs by tagValue: " + ex.getMessage());
        }
    }

    @Override
    public List<AssetTagLog> getLogsByAsset(UUID assetId) {
        try{
            if(assetId == null){
                throw new IllegalArgumentException("AssetId must not be null");
            }
            return assetTagLogRepository.findAllLogsByAssetId(assetId);

        } catch (Exception ex) {
            throw new RuntimeException("Error to get logs by asset: " + ex.getMessage());
        }
    }
}
