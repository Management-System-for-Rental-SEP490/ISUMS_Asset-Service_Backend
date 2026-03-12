package com.isums.assetservice.infrastructures.listeners;

import com.isums.assetservice.domains.events.AssetConditionEvent;
import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetConditionEventListener {

    private final AssetItemService assetItemService;

    @KafkaListener(topics = "asset-condition-update-topic", groupId = "asset-group")
    public void handleAssetConditionUpdate(AssetConditionEvent event){
        System.out.println("Received event: " + event);
        assetItemService.updateCondition(
                event.getAssetId(),
                event.getConditionScore()
        );

    }
}
