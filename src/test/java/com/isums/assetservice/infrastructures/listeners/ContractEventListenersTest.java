package com.isums.assetservice.infrastructures.listeners;

import com.isums.assetservice.domains.dtos.AppAccessChangedEvent;
import com.isums.assetservice.domains.dtos.PowerCutConfirmedEvent;
import com.isums.assetservice.domains.entities.IotController;
import com.isums.assetservice.domains.entities.PowerCutJob;
import com.isums.assetservice.domains.enums.IotControllerStatus;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import com.isums.assetservice.infrastructures.repositories.PowerCutJobRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractEventListeners (asset-service)")
class ContractEventListenersTest {

    @Mock private ObjectMapper objectMapper;
    @Mock private PowerCutJobRepository powerCutJobRepo;
    @Mock private IotControllerRepository controllerRepo;
    @Mock private Acknowledgment ack;

    @InjectMocks private ContractEventListeners listener;

    @Nested
    @DisplayName("handlePowerCutConfirmed")
    class PowerCut {

        private ConsumerRecord<String, String> rec =
                new ConsumerRecord<>("contract.power-cut-confirmed", 0, 0L, "k", "v");

        @Test
        @DisplayName("saves PENDING PowerCutJob on happy path")
        void happy() throws Exception {
            PowerCutConfirmedEvent event = new PowerCutConfirmedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), Instant.now().plusSeconds(86400), "m1");
            when(objectMapper.readValue("v", PowerCutConfirmedEvent.class)).thenReturn(event);

            listener.handlePowerCutConfirmed(rec, ack);

            ArgumentCaptor<PowerCutJob> cap = ArgumentCaptor.forClass(PowerCutJob.class);
            verify(powerCutJobRepo).save(cap.capture());
            assertThat(cap.getValue().getStatus().name()).isEqualTo("PENDING");
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("rethrows RuntimeException on any failure (Kafka retry)")
        void retry() throws Exception {
            when(objectMapper.readValue(any(String.class), eq(PowerCutConfirmedEvent.class)))
                    .thenThrow(new RuntimeException("bad"));

            assertThatThrownBy(() -> listener.handlePowerCutConfirmed(rec, ack))
                    .isInstanceOf(RuntimeException.class);
            verify(ack, never()).acknowledge();
        }
    }

    @Nested
    @DisplayName("handleAppAccessChanged")
    class AppAccess {

        private ConsumerRecord<String, String> rec =
                new ConsumerRecord<>("payment.app-access-changed", 0, 0L, "k", "v");

        @Test
        @DisplayName("acks and skips when restricted=true (event is for setting restriction, not lifting)")
        void restricted() throws Exception {
            AppAccessChangedEvent event = new AppAccessChangedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true, "LATE_PAYMENT", "m1");
            when(objectMapper.readValue("v", AppAccessChangedEvent.class)).thenReturn(event);

            listener.handleAppAccessChanged(rec, ack);

            verify(ack).acknowledge();
            verify(controllerRepo, never()).save(any());
        }

        @Test
        @DisplayName("sets paymentCutActive=false when restricted=false and controller has it active")
        void unrestrict() throws Exception {
            UUID houseId = UUID.randomUUID();
            AppAccessChangedEvent event = new AppAccessChangedEvent(
                    UUID.randomUUID(), houseId, UUID.randomUUID(), false, "PAYMENT_RECEIVED", "m1");
            IotController ctrl = IotController.builder()
                    .id(UUID.randomUUID()).deviceId("d1").houseId(houseId)
                    .status(IotControllerStatus.ACTIVE)
                    .paymentCutActive(true)
                    .activePaymentCutJobId(UUID.randomUUID()).build();

            when(objectMapper.readValue("v", AppAccessChangedEvent.class)).thenReturn(event);
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(ctrl));

            listener.handleAppAccessChanged(rec, ack);

            assertThat(ctrl.isPaymentCutActive()).isFalse();
            assertThat(ctrl.getActivePaymentCutJobId()).isNull();
            verify(controllerRepo).save(ctrl);
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("acks but skips save when paymentCutActive already false (idempotent)")
        void alreadyInactive() throws Exception {
            UUID houseId = UUID.randomUUID();
            AppAccessChangedEvent event = new AppAccessChangedEvent(
                    UUID.randomUUID(), houseId, UUID.randomUUID(), false, "r", "m1");
            IotController ctrl = IotController.builder()
                    .id(UUID.randomUUID()).deviceId("d1").houseId(houseId)
                    .status(IotControllerStatus.ACTIVE).paymentCutActive(false).build();

            when(objectMapper.readValue("v", AppAccessChangedEvent.class)).thenReturn(event);
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(ctrl));

            listener.handleAppAccessChanged(rec, ack);

            verify(controllerRepo, never()).save(any());
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("acks when no controller exists for the house")
        void noController() throws Exception {
            UUID houseId = UUID.randomUUID();
            AppAccessChangedEvent event = new AppAccessChangedEvent(
                    UUID.randomUUID(), houseId, UUID.randomUUID(), false, "r", "m1");

            when(objectMapper.readValue("v", AppAccessChangedEvent.class)).thenReturn(event);
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.empty());

            listener.handleAppAccessChanged(rec, ack);

            verify(controllerRepo, never()).save(any());
            verify(ack).acknowledge();
        }
    }
}
