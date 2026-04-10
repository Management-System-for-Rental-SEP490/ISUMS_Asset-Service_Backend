package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.PowerCutJob;
import com.isums.assetservice.domains.enums.PowerCutJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PowerCutJobRepository extends JpaRepository<PowerCutJob, UUID> {
    List<PowerCutJob> findByStatusAndExecuteAtBefore(PowerCutJobStatus status, Instant executeAt);
}
