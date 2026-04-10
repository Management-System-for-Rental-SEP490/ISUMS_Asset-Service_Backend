package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.PowerCutJobStatus;
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
@Table(name = "power_cut_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerCutJob {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "execute_at", nullable = false)
    private Instant executeAt;

    @Enumerated(EnumType.STRING)
    private PowerCutJobStatus status;

    @CreationTimestamp
    private Instant createdAt;
}