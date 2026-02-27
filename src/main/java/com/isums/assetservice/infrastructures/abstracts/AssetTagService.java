package com.isums.assetservice.infrastructures.abstracts;

import com.isums.assetservice.domains.dtos.AssetTagDto.AssetTagDto;
import com.isums.assetservice.domains.dtos.AssetTagDto.AttachTagRequest;
import com.isums.assetservice.domains.dtos.AssetTagDto.TransferTagRequest;

public interface AssetTagService {
    AssetTagDto attachTag(AttachTagRequest request);
    AssetTagDto detachTag(String tagValue);
    AssetTagDto transferTag(TransferTagRequest request);

}
