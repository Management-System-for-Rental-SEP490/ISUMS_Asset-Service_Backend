package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssetEventRepository extends JpaRepository<AssetEvent, UUID> {
}
