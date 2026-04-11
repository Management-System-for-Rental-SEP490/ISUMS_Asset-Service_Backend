package com.isums.assetservice.infrastructures.listeners;

import com.isums.assetservice.domains.dtos.AppAccessChangedEvent;
import com.isums.assetservice.domains.dtos.PowerCutConfirmedEvent;
import com.isums.assetservice.domains.entities.PowerCutJob;
import com.isums.assetservice.domains.enums.PowerCutJobStatus;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import com.isums.assetservice.infrastructures.repositories.PowerCutJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractEventListeners {

    private final ObjectMapper objectMapper;
    private final PowerCutJobRepository powerCutJobRepository;
    private final IotControllerRepository controllerRepository;

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

    @KafkaListener(topics = "payment.app-access-changed", groupId = "asset-group")
    public void handleAppAccessChanged(
            ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            AppAccessChangedEvent event = objectMapper.readValue(
                    record.value(), AppAccessChangedEvent.class);

            if (event.restricted()) {
                log.debug("[Asset] AppAccessChanged restricted=true houseId={} — ignored", event.houseId());
                ack.acknowledge();
                return;
            }

            controllerRepository.findByHouseId(event.houseId()).ifPresentOrElse(ctrl -> {
                if (!ctrl.isPaymentCutActive()) {
                    log.debug("[Asset] paymentCutActive already false houseId={} — skip", event.houseId());
                    return;
                }
                ctrl.setPaymentCutActive(false);
                ctrl.setActivePaymentCutJobId(null);
                controllerRepository.save(ctrl);
                log.info("[Asset] paymentCutActive=false houseId={} contractId={} reason={} — power can now be restored manually",
                        event.houseId(), event.contractId(), event.reason());
            }, () -> log.warn("[Asset] handleAppAccessChanged — no controller found for houseId={}", event.houseId()));

            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Asset] handleAppAccessChanged failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}