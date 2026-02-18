package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.projections.IoTDeviceView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IoTDeviceRepository extends JpaRepository<IoTDevice, UUID> {

    @Query("""
                select\s
                    d.id as iotDeviceId,
                    d.thing as thing,
                    d.serialNumber as serialNumber,
                    a.id as assetId,
                    a.houseId as houseId,
                    c.id as categoryId,
                    c.code as categoryCode,
                    c.detectionType as detectionType
                from IoTDevice d
                join d.assetItem a
                join a.category c
                where d.thing = :thing
           \s""")
    Optional<IoTDeviceView> findViewByThing(@Param("thing") String thing);

    Optional<IoTDevice> findByThing(String thing);
}
