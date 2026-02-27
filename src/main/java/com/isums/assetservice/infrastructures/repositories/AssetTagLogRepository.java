package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetTagLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetTagLogRepository extends JpaRepository<AssetTagLog, UUID> {
    List<AssetTagLog> findByTagValueOrderByCreatedAtDesc(String tagValue);
    List<AssetTagLog> findByAssetIdOrderByCreatedAtDesc(UUID assetId);
}
