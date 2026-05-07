package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetEventRepository extends JpaRepository<AssetEvent, UUID> {
    @Query("""
    SELECT e FROM AssetEvent e
    JOIN FETCH e.assetItem a
    WHERE e.jobId = :jobId
    ORDER BY e.createdAt DESC
    """)
    List<AssetEvent> findByJobIdWithAsset(UUID jobId);

    @Query("""
    SELECT e FROM AssetEvent e
    WHERE e.assetItem.id = :assetId
    ORDER BY e.createdAt DESC
""")
    List<AssetEvent> findLatestEvent(UUID assetId);


    @Query("""
    SELECT e FROM AssetEvent e
    WHERE e.assetItem.id = :assetId
    AND e.createdAt < (
        SELECT MAX(e2.createdAt) FROM AssetEvent e2
        WHERE e2.assetItem.id = :assetId
    )
    ORDER BY e.createdAt DESC
    """)
    List<AssetEvent> findPreviousEvent(@Param("assetId") UUID assetId);

    Optional<AssetEvent> findTopByAssetItemIdOrderByCreatedAtDesc(UUID assetId);

    @Query("""
    SELECT e FROM AssetEvent e
    WHERE e.assetItem.id = :assetId
      AND e.id <> :excludeEventId
      AND e.jobId IS NULL
      AND e.eventType IS NULL
      AND e.createdAt BETWEEN :from AND :to
    ORDER BY e.createdAt DESC
    """)
    List<AssetEvent> findRecentUnscopedImageReplacementEvents(
            @Param("assetId") UUID assetId,
            @Param("excludeEventId") UUID excludeEventId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

}
