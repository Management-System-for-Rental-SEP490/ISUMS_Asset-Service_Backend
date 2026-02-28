package com.isums.assetservice.infrastructures.repositories;

import com.isums.assetservice.domains.entities.AssetTag;
import com.isums.assetservice.domains.enums.TagType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetTagRepository extends JpaRepository<AssetTag,UUID> {
    Optional<AssetTag> findByTagValueAndIsActiveTrue(String tagValue);

    List<AssetTag> findByAssetItemIdAndIsActiveTrue(UUID assetItemId);

    Boolean existsByTagValueAndIsActiveTrue(String tagValue);
    Boolean existsByAssetItemIdAndTagTypeAndIsActiveTrue(UUID assetId, TagType tagType);

}
