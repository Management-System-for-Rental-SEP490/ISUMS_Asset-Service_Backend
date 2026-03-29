package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.AssetEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assetEvents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID jobId;

    @Enumerated(EnumType.STRING)
    private AssetEventType eventType;

    @Column(columnDefinition = "text")
    private String description;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private UUID createBy;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id")
    private AssetItem assetItem;
}
