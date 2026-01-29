package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssetImageRepository extends JpaRepository<AssetImage, UUID> {
}
