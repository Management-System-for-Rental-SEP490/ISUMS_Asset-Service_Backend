package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetEventImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetEventImageRepository extends JpaRepository<AssetEventImage, UUID> {

    List<AssetEventImage> findByEventIdAndAssetId(UUID eventId, UUID assetId);

    List<AssetEventImage> findByEventId(UUID eventId);
}
