package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AssetTagDto.AssetTagDto;
import com.isums.assetservice.domains.dtos.AssetTagDto.AttachTagRequest;
import com.isums.assetservice.domains.dtos.AssetTagDto.TransferTagRequest;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.AssetTag;
import com.isums.assetservice.domains.entities.AssetTagLog;
import com.isums.assetservice.domains.enums.TagAction;
import com.isums.assetservice.infrastructures.abstracts.AssetTagService;
import com.isums.assetservice.infrastructures.mapper.AssetMapper;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import com.isums.assetservice.infrastructures.repositories.AssetTagLogRepository;
import com.isums.assetservice.infrastructures.repositories.AssetTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AssetTagServiceImpl implements AssetTagService {
    private final AssetTagRepository assetTagRepository;
    private final AssetItemRepository assetItemRepository;
    private final AssetTagLogRepository assetTagLogRepository;
    private final AssetMapper assetMapper;


    @Override
    public AssetTagDto attachTag(AttachTagRequest request) {
        try{
            AssetItem assetItem = assetItemRepository.findById(request.assetId())
                    .orElseThrow(()-> new RuntimeException("Asset not found"));

            if(assetTagRepository.existsByTagValueAndIsActiveTrue(request.tagValue())){
                throw new RuntimeException("Tag now is in-use");
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

    @Override
    public AssetTagDto detachTag(String tagValue) {
        try{
            AssetTag tag = assetTagRepository.findByTagValueAndIsActiveTrue(tagValue)
                    .orElseThrow(()-> new RuntimeException("Active tag not found"));

            tag.setActive(false);
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
            currentTag.setActive(false);
            currentTag.setDeactivatedAt(Instant.now());
            assetTagRepository.save(currentTag);

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
}
