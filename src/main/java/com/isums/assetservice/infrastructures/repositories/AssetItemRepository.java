package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.dtos.AssetCountByFunctionAreaDto;
import com.isums.assetservice.domains.entities.AssetItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AssetItemRepository extends JpaRepository<AssetItem, UUID>, JpaSpecificationExecutor<AssetItem> {

    @EntityGraph(attributePaths = {"category", "images", "events"})
    List<AssetItem> findByHouseId(UUID houseId);

    @EntityGraph(attributePaths = {"category", "images", "events"})
    List<AssetItem> findByHouseIdAndFunctionAreaId(UUID houseId, UUID functionAreaId);

    @Query("""
            select new com.isums.assetservice.domains.dtos.AssetCountByFunctionAreaDto(
                a.functionAreaId,
                count(a.id)
            )
            from AssetItem a
            where a.houseId = :houseId
              and a.functionAreaId is not null
            group by a.functionAreaId
            """)
    List<AssetCountByFunctionAreaDto> countByHouseIdGroupByFunctionAreaId(UUID houseId);
}
