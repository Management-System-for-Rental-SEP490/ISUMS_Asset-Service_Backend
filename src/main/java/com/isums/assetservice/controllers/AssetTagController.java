package com.isums.assetservice.controllers;

import com.google.protobuf.Api;
import com.isums.assetservice.domains.dtos.ApiResponse;
import com.isums.assetservice.domains.dtos.ApiResponses;
import com.isums.assetservice.domains.dtos.AssetItemDTO.AssetItemDto;
import com.isums.assetservice.domains.dtos.AssetTagDto.AssetTagDto;
import com.isums.assetservice.domains.dtos.AssetTagDto.AttachTagRequest;
import com.isums.assetservice.domains.dtos.AssetTagDto.TransferTagRequest;
import com.isums.assetservice.domains.entities.AssetTag;
import com.isums.assetservice.infrastructures.abstracts.AssetTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/assets/tags")
@RequiredArgsConstructor
public class AssetTagController {
    private final AssetTagService assetTagService;

    //attach
    @PostMapping
    public ApiResponse<AssetTagDto> attachTag(@RequestBody AttachTagRequest request){
        AssetTagDto res = assetTagService.attachTag(request);
        return ApiResponses.created(res,"Attach tag successfully");
    }
    //detach
    @PutMapping("/detach/{tagValue}")
    public ApiResponse<AssetTagDto> detachTag(@PathVariable String tagValue){
        AssetTagDto res = assetTagService.detachTag(tagValue);
        return ApiResponses.ok(res,"Detach tag successfully");
    }

    //transfer
    @PutMapping("/transfer")
    public ApiResponse<AssetTagDto> transferTag(@RequestBody TransferTagRequest request){
        AssetTagDto res = assetTagService.transferTag(request);
        return ApiResponses.ok(res,"Transfer tag successfully");
    }

    @GetMapping("/asset/{tagValue}")
    public ApiResponse<AssetItemDto> getAssetItemByTagValue(@PathVariable String tagValue){
        AssetItemDto res = assetTagService.getAssetItemByTagValue(tagValue);
        return ApiResponses.ok(res,"Get asset item successfully");
    }
}
