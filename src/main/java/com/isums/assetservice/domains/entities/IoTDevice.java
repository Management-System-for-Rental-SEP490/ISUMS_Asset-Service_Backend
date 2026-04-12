package com.isums.assetservice.domains.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "iot_devices",
        indexes = {
                @Index(name = "idx_iot_thing", columnList = "thing"),
                @Index(name = "idx_iot_serial", columnList = "serial_number"),
                @Index(name = "idx_iot_asset_item", columnList = "asset_item_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class IoTDevice {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String thing; //name

    @Column(name = "serial_number", unique = true, nullable = false)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_item_id", nullable = false)
    private AssetItem assetItem;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "iot_device_capabilities",
            joinColumns = @JoinColumn(name = "iotdevice_id")
    )
    @Column(name = "capability")
    @Builder.Default
    private Set<String> capabilities = new HashSet<>();
}
