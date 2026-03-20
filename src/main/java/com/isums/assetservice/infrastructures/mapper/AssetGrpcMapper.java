package com.isums.assetservice.infrastructures.mapper;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import com.isums.assetservice.domains.entities.AssetCategory;
import com.isums.assetservice.domains.entities.AssetEvent;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.enums.AssetStatus;
import com.isums.assetservice.grpc.AssetCategoryDto;
import com.isums.assetservice.grpc.AssetEventDto;
import com.isums.assetservice.grpc.AssetItemDto;
import com.isums.assetservice.grpc.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

import com.isums.assetservice.domains.enums.AssetEventType;


@Component
@RequiredArgsConstructor
public final class AssetGrpcMapper {

    public AssetItemDto toDto(AssetItem item) {
        AssetItemDto.Builder b = AssetItemDto.newBuilder()
                .setId(uuid(item.getId()))
                .setHouseId(uuid(item.getHouseId()))
                .setDisplayName(str(item.getDisplayName()))
                .setSerialNumber(str(item.getSerialNumber()))
                .setConditionPercent(item.getConditionPercent())
                .setStatus(mapStatus(item.getStatus()));

        if (item.getCategory() != null) {
            b.setCategory(toCategoryDto(item.getCategory()));
        }

        if (item.getImages() != null) {
            for (AssetImage img : item.getImages()) {
                b.addImages(toImageDto(img));
            }
        }

        if (item.getEvents() != null) {
            for (AssetEvent e : item.getEvents()) {
                b.addEvents(toEventDto(e));
            }
        }

        return b.build();
    }

    private AssetCategoryDto toCategoryDto(AssetCategory c) {
        return AssetCategoryDto.newBuilder()
                .setId(uuid(c.getId()))
                .setName(str(c.getName()))
                .setCompensationPercent(c.getCompensationPercent())
                .setDescription(str(c.getDescription()))
                .build();
    }

    private AssetImageDto toImageDto(AssetImage img) {
        return AssetImageDto.newBuilder()
                .setId(uuid(img.getId()))
                .setImageUrl(str(img.getImageUrl()))
                .setNote(str(img.getNote()))
                .setCreatedAt(ts(img.getCreatedAt()))
                .build();
    }

    private AssetEventDto toEventDto(AssetEvent e) {
        return AssetEventDto.newBuilder()
                .setId(uuid(e.getId()))
                .setEventType(mapEventType(e.getEventType()))
                .setDescription(str(e.getDescription()))
                .setCreatedAt(ts(e.getCreatedAt()))
                .setCreatedBy(uuid(e.getCreateBy()))
                .build();
    }

    private com.isums.assetservice.grpc.AssetStatus mapStatus(AssetStatus s) {

        if (s == null) {
            return com.isums.assetservice.grpc.AssetStatus.ASSET_STATUS_UNSPECIFIED;
        }

        return switch (s) {

            case AVAILABLE -> com.isums.assetservice.grpc.AssetStatus.ASSET_STATUS_AVAILABLE;
            case IN_USE -> com.isums.assetservice.grpc.AssetStatus.ASSET_STATUS_IN_USE;
            case ACTIVE -> com.isums.assetservice.grpc.AssetStatus.ASSET_STATUS_UNSPECIFIED;
            case BROKEN -> com.isums.assetservice.grpc.AssetStatus.ASSET_STATUS_BROKEN;
            case DISPOSED -> com.isums.assetservice.grpc.AssetStatus.ASSET_STATUS_DISPOSED;
            case DELETED -> com.isums.assetservice.grpc.AssetStatus.ASSET_STATUS_DELETED;
        };
    }

    private com.isums.assetservice.grpc.AssetEventType mapEventType(AssetEventType t) {

        if (t == null) {
            return com.isums.assetservice.grpc.AssetEventType.ASSET_EVENT_TYPE_UNSPECIFIED;
        }

        return switch (t) {
            case CREATED -> com.isums.assetservice.grpc.AssetEventType.ASSET_EVENT_TYPE_CREATED;
            case CHECKED -> com.isums.assetservice.grpc.AssetEventType.ASSET_EVENT_TYPE_CHECKED;
            case DAMAGED -> com.isums.assetservice.grpc.AssetEventType.ASSET_EVENT_TYPE_DAMAGED;
            case REPAIRED -> com.isums.assetservice.grpc.AssetEventType.ASSET_EVENT_TYPE_REPAIRED;
            case REPLACED -> com.isums.assetservice.grpc.AssetEventType.ASSET_EVENT_TYPE_REPLACED;
            case DISPOSED -> com.isums.assetservice.grpc.AssetEventType.ASSET_EVENT_TYPE_DISPOSED;
            case TRANSFERRED -> com.isums.assetservice.grpc.AssetEventType.ASSET_EVENT_TYPE_TRANSFERRED;
        };
    }

    private static String str(String v) { return v == null ? "" : v; }
    private static String uuid(UUID v) { return v == null ? "" : v.toString(); }

    private static Timestamp ts(Instant i) {
        return i == null ? Timestamp.getDefaultInstance() : Timestamps.fromMillis(i.toEpochMilli());
    }
}
