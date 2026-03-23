package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetImageRepository extends JpaRepository<AssetImage, UUID> {
    List<AssetImage> findByAssetItemId(UUID assetItemId);
}
