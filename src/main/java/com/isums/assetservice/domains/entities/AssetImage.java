package com.isums.assetservice.domains.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assetImages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetImage {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String imageUrl;

    private String note;

    private Instant createAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id",nullable = false)
    private AssetItem assetItem;
}
