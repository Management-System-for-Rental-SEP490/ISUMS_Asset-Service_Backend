package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssetItemRepository extends JpaRepository<AssetItem, UUID> {
}
