package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetEventImage;
import com.isums.assetservice.domains.enums.AssetEventImageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetEventImageRepository extends JpaRepository<AssetEventImage, UUID> {
    List<AssetEventImage> findByEventId(UUID eventId);

    List<AssetEventImage> findByEventIdOrderByCreatedAtAsc(UUID eventId);

    /** All images of a given type for this event, oldest first. Used by
     *  the read path to return BEFORE and AFTER arrays separately. */
    List<AssetEventImage> findByEventIdAndTypeOrderByCreatedAtAsc(UUID eventId, AssetEventImageType type);
}

