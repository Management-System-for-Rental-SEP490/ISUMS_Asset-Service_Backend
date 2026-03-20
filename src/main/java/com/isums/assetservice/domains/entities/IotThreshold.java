package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.Severity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "iot_thresholds",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"house_id", "area_id", "metric"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "area_id")
    private UUID areaId; // null = house-level

    @Column(nullable = false, length = 50)
    private String metric;

    @Column(name = "min_val")
    private Double minVal;

    @Column(name = "max_val")
    private Double maxVal;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity = Severity.WARNING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
