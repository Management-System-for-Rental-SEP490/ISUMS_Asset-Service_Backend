package com.isums.assetservice.infrastructures.listeners;

import com.isums.assetservice.domains.dtos.PowerCutConfirmedEvent;
import com.isums.assetservice.domains.entities.PowerCutJob;
import com.isums.assetservice.domains.enums.PowerCutJobStatus;
import com.isums.assetservice.infrastructures.repositories.PowerCutJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractEventListeners {

    private final ObjectMapper objectMapper;
    private final PowerCutJobRepository powerCutJobRepository;

    @KafkaListener(topics = "contract.power-cut-confirmed", groupId = "asset-group")
    public void handlePowerCutConfirmed(
            ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            PowerCutConfirmedEvent event = objectMapper.readValue(
                    record.value(), PowerCutConfirmedEvent.class);

            powerCutJobRepository.save(PowerCutJob.builder()
                    .houseId(event.getHouseId())
                    .contractId(event.getContractId())
                    .executeAt(event.getExecuteAt())
                    .status(PowerCutJobStatus.PENDING)
                    .build());

            log.info("[Asset] PowerCutJob saved houseId={} executeAt={}",
                    event.getHouseId(), event.getExecuteAt());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Asset] handlePowerCutConfirmed failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
