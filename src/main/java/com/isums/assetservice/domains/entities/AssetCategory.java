package com.isums.assetservice.domains.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.isums.assetservice.domains.enums.DetectionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assetCategories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCategory {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(columnDefinition = "text")
    private String name;

    private String code;

    @Column(name = "compensation_percent")
    private int compensationPercent;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "detection_type")
    @Enumerated(EnumType.STRING)
    private DetectionType detectionType;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssetItem> assetItems = new ArrayList<>();
}
