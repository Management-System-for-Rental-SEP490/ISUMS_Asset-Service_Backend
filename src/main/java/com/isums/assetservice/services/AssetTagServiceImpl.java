package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetImageDto;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetTagDto.AssetTagDto;
import com.isums.assetservice.domains.dtos.AssetTagDto.AttachTagRequest;
import com.isums.assetservice.domains.dtos.AssetTagDto.TransferTagRequest;
import com.isums.assetservice.domains.entities.AssetImage;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.AssetTag;
import com.isums.assetservice.domains.entities.AssetTagLog;
import com.isums.assetservice.domains.enums.TagAction;
import com.isums.assetservice.infrastructures.abstracts.AssetTagService;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetImageRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import com.isums.assetservice.infrastructures.repositories.AssetTagLogRepository;
import com.isums.assetservice.infrastructures.repositories.AssetTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetTagServiceImpl implements AssetTagService {
    private final AssetTagRepository assetTagRepository;
    private final AssetItemRepository assetItemRepository;
    private final AssetTagLogRepository assetTagLogRepository;
    private final AssetImageRepository assetImageRepository;
    private final AssetMapper assetMapper;
    private final S3ServiceImpl s3;


    @Override
    public AssetTagDto attachTag(AttachTagRequest request) {
        try{
            AssetItem assetItem = assetItemRepository.findById(request.assetId())
                    .orElseThrow(()-> new RuntimeException("Asset not found"));

            if(assetTagRepository.existsByTagValueAndIsActiveTrue(request.tagValue())){
                throw new RuntimeException("Tag/QR now is in-use");
            }

            if (assetTagRepository.existsByAssetItemIdAndTagTypeAndIsActiveTrue(
                    assetItem.getId(), request.tagType())) {
                throw new RuntimeException("Asset already has active tag of this type");
            }

            AssetTag assetTag = AssetTag.builder()
                    .tagValue(request.tagValue())
                    .tagType(request.tagType())
                    .assetItem(assetItem)
                    .isActive(true)
                    .activatedAt(Instant.now())
                    .build();

            AssetTag created = assetTagRepository.save(assetTag);

            AssetTagLog log = AssetTagLog.builder()
                    .tagValue(request.tagValue())
                    .oldAssetId(null)
                    .newAssetId(assetItem.getId())
                    .oldHouseId(null)
                    .newHouseId(assetItem.getHouseId())
                    .tagAction(TagAction.ATTACHED)
                    .createdAt(Instant.now())
                    .build();

            assetTagLogRepository.save(log);

            return assetMapper.tagDto(created);


        } catch (Exception ex) {
            throw new RuntimeException("Error to attach tag" + ex.getMessage());
        }
    }

    @Transactional
    @Override
    public AssetTagDto detachTag(String tagValue) {
        try{
            AssetTag tag = assetTagRepository.findByTagValueAndIsActiveTrue(tagValue)
                    .orElseThrow(()-> new RuntimeException("Active tag not found"));

            tag.setIsActive(false);
            tag.setDeactivatedAt(Instant.now());

            assetTagRepository.save(tag);

            AssetTagLog log = AssetTagLog.builder()
                    .tagValue(tag.getTagValue())
                    .oldAssetId(tag.getAssetItem().getId())
                    .newAssetId(null)
                    .oldHouseId(tag.getAssetItem().getHouseId())
                    .newHouseId(null)
                    .tagAction(TagAction.DETACHED)
                    .createdAt(Instant.now())
                    .build();

            assetTagLogRepository.save(log);

            return assetMapper.tagDto(tag);

        } catch (Exception ex) {
            throw new RuntimeException("Error to detach tag"+ ex.getMessage());
        }
    }

    @Transactional
    @Override
    public AssetTagDto transferTag(TransferTagRequest request) {
        try{
            if(request.tagValue() == null || request.tagValue().isBlank()){
                throw new IllegalArgumentException("Tag value must not be empty");
            }

            AssetTag currentTag = assetTagRepository.findByTagValueAndIsActiveTrue(request.tagValue())
                    .orElseThrow(()-> new RuntimeException("Active tag not found"));

            AssetItem oldAsset = currentTag.getAssetItem();

            AssetItem newAsset = assetItemRepository.findById(request.newAssetId())
                    .orElseThrow(() -> new RuntimeException("New asset not found"));

            if(oldAsset.getId().equals(newAsset.getId())){
                throw new RuntimeException("Tag already attached to this asset");
            }

            //deactivated tag cũ
            currentTag.setIsActive(false);
            currentTag.setDeactivatedAt(Instant.now());
            assetTagRepository.save(currentTag);

            if (assetTagRepository.existsByAssetItemIdAndTagTypeAndIsActiveTrue(
                    newAsset.getId(), currentTag.getTagType())) {
                throw new RuntimeException("Asset already has active tag of this type");
            }

            //attach tag mới
            AssetTag newTag = AssetTag.builder()
                    .tagValue(currentTag.getTagValue())
                    .tagType(currentTag.getTagType())
                    .assetItem(newAsset)
                    .isActive(true)
                    .activatedAt(Instant.now())
                    .build();

            assetTagRepository.save(newTag);

            //log new tag
            AssetTagLog newTagLog = AssetTagLog.builder()
                    .tagValue(newTag.getTagValue())
                    .tagAction(TagAction.TRANSFERRED)
                    .oldAssetId(oldAsset.getId())
                    .newAssetId(newAsset.getId())
                    .oldHouseId(oldAsset.getHouseId())
                    .newHouseId(newAsset.getHouseId())
                    .createdAt(Instant.now())
                    .build();

            assetTagLogRepository.save(newTagLog);

            return assetMapper.tagDto(newTag);
        } catch (Exception ex) {
            throw new RuntimeException("Error to transfer tag to new asset" +ex.getMessage());
        }
    }

    @Transactional
    @Override
    public AssetItemDto getAssetItemByTagValue(String tagValue) {
        try {
            if (tagValue == null || tagValue.isBlank()) {
                throw new IllegalArgumentException("Tag value must not be empty");
            }

            AssetTag tag = assetTagRepository
                    .findByTagValueAndIsActiveTrue(tagValue)
                    .orElseThrow(() -> new RuntimeException("Tag not found or not active"));

            AssetItem item = tag.getAssetItem();

            List<AssetTag> tags = assetTagRepository
                    .findByAssetItemIdAndIsActiveTrue(item.getId());

            AssetItemDto dto = assetMapper.mapAssetItem(item);

            dto.setTags(assetMapper.tagDtos(tags));

            dto.setImages(getAssetImages(item.getId()));

            return dto;

        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item information: " + ex.getMessage());
        }
    }

    private List<AssetImageDto> getAssetImages(UUID assetId) {
        List<AssetImage> images = assetImageRepository.findByAssetItemId(assetId);

        List<AssetImageDto> imageDto = new ArrayList<>();
        images.forEach(image ->{
            String url = s3.getImageUrl(image.getKey());
            imageDto.add(new AssetImageDto(image.getId(),url,image.getCreatedAt()));
        });

        return imageDto;
    }
}
