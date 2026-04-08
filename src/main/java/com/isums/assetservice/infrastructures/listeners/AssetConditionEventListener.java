package com.isums.assetservice.infrastructures.listeners;

import com.isums.assetservice.domains.events.AssetConditionEvent;
import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssetConditionEventListener {

    private final AssetItemService assetItemService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "asset-condition-update-topic", groupId = "asset-group")
    public void handleAssetConditionUpdate(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            AssetConditionEvent event = objectMapper.readValue(record.value(), AssetConditionEvent.class);
            assetItemService.updateCondition(event.getAssetId(), event.getConditionScore());
            ack.acknowledge();
            log.info("[Asset] Condition updated assetId={} score={}", event.getAssetId(), event.getConditionScore());
        } catch (tools.jackson.core.JacksonException e) {
            log.error("[Asset] Deserialize failed raw={}: {}", record.value(), e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Asset] handleAssetConditionUpdate failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
