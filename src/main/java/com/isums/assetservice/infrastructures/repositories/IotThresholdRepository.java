package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.IotThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IotThresholdRepository extends JpaRepository<IotThreshold, UUID> {
    List<IotThreshold> findByHouseIdAndAreaIdIsNull(UUID houseId);

    List<IotThreshold> findByAreaId(UUID areaId);

    List<IotThreshold> findByHouseId(UUID houseId);

    Optional<IotThreshold> findByHouseIdAndAreaIdIsNullAndMetric(UUID houseId, String metric);

    Optional<IotThreshold> findByAreaIdAndMetric(UUID areaId, String metric);

    void deleteByHouseIdAndAreaIdIsNullAndMetric(UUID houseId, String metric);

    void deleteByAreaIdAndMetric(UUID areaId, String metric);
}
