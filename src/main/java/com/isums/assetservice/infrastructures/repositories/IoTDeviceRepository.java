package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.projections.IoTDeviceView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
                     a.functionAreaId as areaId,
                     c.id as categoryId,
                     c.code as categoryCode
                 from IoTDevice d
                 join d.assetItem a
                 join a.category c
                 where d.thing = :thing
            \s""")
    Optional<IoTDeviceView> findViewByThing(@Param("thing") String thing);

    Optional<IoTDevice> findByThing(String thing);

    Optional<IoTDevice> findBySerialNumber(String serialNumber);

    List<IoTDevice> findByAssetItem_HouseId(UUID houseId);

    @Query("""
            SELECT d FROM IoTDevice d
            JOIN d.assetItem ai
            WHERE ai.functionAreaId = :areaId
            AND :capability MEMBER OF d.capabilities
            """)
    List<IoTDevice> findByAreaIdAndCapability(
            @Param("areaId") UUID areaId,
            @Param("capability") String capability
    );
}
