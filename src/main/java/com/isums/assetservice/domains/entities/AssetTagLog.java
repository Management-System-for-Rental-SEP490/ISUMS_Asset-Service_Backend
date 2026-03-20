package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.TagAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "asset_tag_logs",
indexes = {
        @Index(name = "idx_tag_value",columnList = "tagValue"),
        @Index(name = "idx_old_house_id",columnList = "oldHouseId"),
        @Index(name = "idx_old_asset_id", columnList = "oldAssetId"),
        @Index(name = "idx_new_asset_id", columnList = "newAssetId"),
        @Index(name = "idx_new_house_id", columnList = "newHouseId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetTagLog {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String tagValue;

    private UUID oldAssetId;
    private UUID newAssetId;

    private UUID oldHouseId;
    private UUID newHouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagAction tagAction;

    private Instant createdAt;

}
