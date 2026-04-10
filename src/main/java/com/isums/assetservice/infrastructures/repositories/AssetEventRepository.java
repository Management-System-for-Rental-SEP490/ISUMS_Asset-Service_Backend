package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AssetEventRepository extends JpaRepository<AssetEvent, UUID> {
    @Query("""
    SELECT e FROM AssetEvent e
    JOIN FETCH e.assetItem a
    WHERE e.jobId = :jobId
    ORDER BY e.createdAt DESC
    """)
    List<AssetEvent> findByJobIdWithAsset(UUID jobId);
}
