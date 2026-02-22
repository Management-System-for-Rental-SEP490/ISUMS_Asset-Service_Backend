package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.AssetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.*;

@Entity
@Table(name = "asset_items",
        indexes = {
                @Index(name = "idx_asset_house", columnList = "house_id"),
                @Index(name = "idx_asset_serial", columnList = "serial_number")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "house_id")
    private UUID houseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private AssetCategory category;

    @Column(name = "display_name", columnDefinition = "text")
    private String displayName;

    @Column(name = "serial_number", columnDefinition = "text")
    private String serialNumber;

    @Column(name = "nfc_id")
    private String nfcId;

    @Column(name = "condition_percent")
    private int conditionPercent;

    @Enumerated(EnumType.STRING)
    private AssetStatus status;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "assetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<AssetImage> images = new LinkedHashSet<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "assetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<AssetEvent> events = new LinkedHashSet<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "assetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<IoTDevice> iotDevices = new LinkedHashSet<>();
}
