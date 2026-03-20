package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetTagLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AssetTagLogRepository extends JpaRepository<AssetTagLog, UUID> {
    List<AssetTagLog> findByTagValueOrderByCreatedAtDesc(String tagValue);
    @Query("""
        SELECT l FROM AssetTagLog l
        WHERE l.oldAssetId = :assetId
           OR l.newAssetId = :assetId
        ORDER BY l.createdAt DESC
    """)
    List<AssetTagLog> findAllLogsByAssetId(UUID assetId);
}
