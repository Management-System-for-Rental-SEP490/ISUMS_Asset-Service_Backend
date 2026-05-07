package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.IotSafetyConfigVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IotSafetyConfigVersionRepository
        extends JpaRepository<IotSafetyConfigVersion, UUID> {

    @Query("""
        SELECT v FROM IotSafetyConfigVersion v
        WHERE v.expiredAt IS NULL
        ORDER BY v.effectiveFrom DESC
    """)
    Optional<IotSafetyConfigVersion> findActive();

    @Query("""
        SELECT v FROM IotSafetyConfigVersion v
        ORDER BY v.effectiveFrom DESC
    """)
    List<IotSafetyConfigVersion> findHistory();
}
