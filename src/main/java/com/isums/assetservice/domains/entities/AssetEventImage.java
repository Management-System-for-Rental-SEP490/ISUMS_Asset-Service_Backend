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
@Table(name = "asset_event_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetEventImage {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String key;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private AssetEvent event;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    private Instant createdAt;
}