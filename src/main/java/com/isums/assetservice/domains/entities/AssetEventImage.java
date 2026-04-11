package com.isums.assetservice.domains.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
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

    private String key;

    @CreationTimestamp
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "event_id",nullable = false)
    private AssetEvent event;
}
