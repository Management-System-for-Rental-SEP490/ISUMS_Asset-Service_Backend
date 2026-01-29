package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.AssetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assetItems")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID houseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private AssetCategory category;

    @Column(columnDefinition = "text")
    private String displayName;

    @Column(columnDefinition = "text")
    private String serialNumber;

    private String nfcId;

    private int conditionPercent;

    @Enumerated(EnumType.STRING)
    private AssetStatus status;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "assetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssetImage> images = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "assetEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssetEvent> events = new ArrayList<>();
}
