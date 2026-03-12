package com.isums.assetservice.domains.entities;

import com.isums.assetservice.domains.enums.IotControllerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iot_controllers", indexes = {
        @Index(name = "idx_ctrl_house", columnList = "house_id"),
        @Index(name = "idx_ctrl_device", columnList = "device_id")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class IotController {

    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @Column(name = "device_id", unique = true, nullable = false)
    private String deviceId; // MAC từ QR

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "thing_name", unique = true)
    private String thingName; // ctrl-aabbccddeeff

    @Column(name = "certificate_arn")
    private String certificateArn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IotControllerStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}