package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.TagType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "asset_tags")
@Builder
public class AssetTag {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private AssetItem assetItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagType tagType;

    @Column(nullable = false)
    private String tagValue;

    @Column(nullable = false)
    private boolean isActive = true;

    private Instant activatedAt;

    private Instant deactivatedAt;
}
