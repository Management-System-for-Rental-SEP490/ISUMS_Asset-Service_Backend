package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.AssetEventImageType;
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

    /**
     * {@code BEFORE} = snapshot of the asset image at event creation
     * time; {@code AFTER} = photo uploaded after the event was
     * processed. Nullable for backward-compat with rows inserted
     * before this discriminator existed — the read side treats them
     * as AFTER by convention (closest match to legacy semantics).
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private AssetEventImageType type;

    private Instant createdAt;
}
