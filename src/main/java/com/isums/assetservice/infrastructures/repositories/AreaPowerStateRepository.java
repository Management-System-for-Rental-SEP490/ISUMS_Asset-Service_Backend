package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AreaPowerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AreaPowerStateRepository extends JpaRepository<AreaPowerState, UUID> {

    Optional<AreaPowerState> findByHouseIdAndAreaId(UUID houseId, UUID areaId);

    List<AreaPowerState> findAllByHouseId(UUID houseId);

    @Modifying
    @Query("""
            UPDATE AreaPowerState s
            SET s.powered = false,
                s.cutReason = com.isums.assetservice.domains.enums.PowerCutReason.PAYMENT_DUE,
                s.powerCutJobId = :jobId,
                s.changedBy = null,
                s.changedAt = CURRENT_TIMESTAMP
            WHERE s.houseId = :houseId
            """)
    int markAllAreasPaymentDue(@Param("houseId") UUID houseId, @Param("jobId") UUID jobId);
}