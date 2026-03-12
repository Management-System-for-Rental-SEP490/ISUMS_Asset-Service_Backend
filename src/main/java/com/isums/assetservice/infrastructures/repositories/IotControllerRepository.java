package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.IotController;
import com.isums.assetservice.domains.enums.IotControllerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IotControllerRepository extends JpaRepository<IotController, UUID> {
    Optional<IotController> findByDeviceId(String deviceId);
    Optional<IotController> findByThingName(String thingName);
    boolean existsByHouseId(UUID houseId);
    Optional<IotController> findByHouseIdAndStatus(UUID houseId, IotControllerStatus status);
}
