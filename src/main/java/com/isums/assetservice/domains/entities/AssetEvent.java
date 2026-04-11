package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.AssetEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.*;

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

    private Integer previousCondition;

    private Integer currentCondition;

    @Column(columnDefinition = "text")
    private String note;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private UUID createBy;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<AssetEventImage> images = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id")
    private AssetItem assetItem;

    public void addImage(AssetEventImage image) {
        this.images.add(image);
        image.setEvent(this);
    }
}

