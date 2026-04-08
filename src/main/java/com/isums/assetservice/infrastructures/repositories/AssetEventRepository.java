package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetEventRepository extends JpaRepository<AssetEvent, UUID> {
    List<AssetEvent> findByJobId(UUID jobId);
}
