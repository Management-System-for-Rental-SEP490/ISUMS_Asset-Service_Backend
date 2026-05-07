package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.PowerCutReason;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "area_power_states",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_area_power_house_area",
                columnNames = {"house_id", "area_id"}
        ),
        indexes = {
                @Index(name = "idx_aps_house", columnList = "house_id"),
                @Index(name = "idx_aps_area",  columnList = "area_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaPowerState {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Column(name = "powered", nullable = false)
    @Builder.Default
    private boolean powered = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "cut_reason")
    private PowerCutReason cutReason;

    @Column(name = "power_cut_job_id")
    private UUID powerCutJobId;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
