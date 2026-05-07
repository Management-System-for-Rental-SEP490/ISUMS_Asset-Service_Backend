package com.isums.assetservice.services;

import com.isums.assetservice.domains.dtos.AreaPowerStateResponse;
import com.isums.assetservice.domains.entities.AreaPowerState;
import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.entities.IotController;
import com.isums.assetservice.domains.enums.IotControllerStatus;
import com.isums.assetservice.domains.enums.NodeCapability;
import com.isums.assetservice.domains.enums.PowerAction;
import com.isums.assetservice.domains.enums.PowerCutReason;
import com.isums.assetservice.exceptions.NotFoundException;
import com.isums.assetservice.infrastructures.repositories.AreaPowerStateRepository;
import com.isums.assetservice.infrastructures.repositories.IoTDeviceRepository;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AreaPowerServiceImpl")
class AreaPowerServiceImplTest {

    @Mock private IotControllerRepository controllerRepo;
    @Mock private IoTDeviceRepository deviceRepo;
    @Mock private AreaPowerStateRepository stateRepo;
    @Mock private IotDataPlaneClient iotClient;

    private AreaPowerServiceImpl service;

    private UUID houseId;
    private UUID areaId;
    private UUID requesterId;

    @BeforeEach
    void setUp() {
        // Construct manually so we can inject a REAL ObjectMapper (mock would return null
        // for writeValueAsString and cause NPE in sendRelayCommands).
        service = new AreaPowerServiceImpl(
                controllerRepo, deviceRepo, stateRepo, iotClient, new ObjectMapper());
        houseId = UUID.randomUUID();
        areaId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
    }

    private IotController activeController() {
        return IotController.builder()
                .id(UUID.randomUUID()).deviceId("ctrl-1").houseId(houseId)
                .thingName("thing-1").status(IotControllerStatus.ACTIVE)
                .paymentCutActive(false).build();
    }

    private IoTDevice relayNode() {
        return IoTDevice.builder()
                .id(UUID.randomUUID()).thing("node-1").serialNumber("SER-1")
                .assetItem(AssetItem.builder().id(UUID.randomUUID()).build())
                .build();
    }

    @Nested
    @DisplayName("toggleAreaPower")
    class Toggle {

        @Test
        @DisplayName("sends MQTT relay_off + persists state when OFF")
        void off() {
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(activeController()));
            when(deviceRepo.findByAreaIdAndCapability(areaId, NodeCapability.RELAY.name()))
                    .thenReturn(List.of(relayNode()));
            when(stateRepo.findByHouseIdAndAreaId(houseId, areaId)).thenReturn(Optional.empty());
            when(stateRepo.save(any(AreaPowerState.class))).thenAnswer(a -> a.getArgument(0));

            AreaPowerStateResponse res = service.toggleAreaPower(houseId, areaId, PowerAction.OFF, requesterId);

            assertThat(res.powered()).isFalse();
            assertThat(res.cutReason()).isEqualTo(PowerCutReason.MANUAL);
            verify(iotClient).publish(any(Consumer.class));
        }

        @Test
        @DisplayName("sends relay_on + clears cutReason when ON")
        void on() {
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(activeController()));
            when(deviceRepo.findByAreaIdAndCapability(areaId, NodeCapability.RELAY.name()))
                    .thenReturn(List.of(relayNode()));
            when(stateRepo.findByHouseIdAndAreaId(houseId, areaId)).thenReturn(Optional.empty());
            when(stateRepo.save(any(AreaPowerState.class))).thenAnswer(a -> a.getArgument(0));

            AreaPowerStateResponse res = service.toggleAreaPower(houseId, areaId, PowerAction.ON, requesterId);

            assertThat(res.powered()).isTrue();
            assertThat(res.cutReason()).isNull();
        }

        @Test
        @DisplayName("throws PAYMENT_REQUIRED when paymentCutActive is true")
        void paymentCutActive() {
            IotController ctrl = activeController();
            ctrl.setPaymentCutActive(true);
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(ctrl));

            assertThatThrownBy(() -> service.toggleAreaPower(houseId, areaId, PowerAction.ON, requesterId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("402");
            verifyNoInteractions(iotClient);
        }

        @Test
        @DisplayName("throws NotFoundException when no controller for house")
        void noController() {
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.toggleAreaPower(houseId, areaId, PowerAction.OFF, requesterId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws SERVICE_UNAVAILABLE when controller status not ACTIVE")
        void inactiveController() {
            IotController ctrl = activeController();
            ctrl.setStatus(IotControllerStatus.OFFLINE);
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(ctrl));

            assertThatThrownBy(() -> service.toggleAreaPower(houseId, areaId, PowerAction.OFF, requesterId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("503");
        }

        @Test
        @DisplayName("throws NotFoundException when no RELAY devices in area")
        void noRelayDevices() {
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(activeController()));
            when(deviceRepo.findByAreaIdAndCapability(areaId, NodeCapability.RELAY.name()))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.toggleAreaPower(houseId, areaId, PowerAction.OFF, requesterId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("RELAY");
        }
    }

    @Nested
    @DisplayName("getAreaPowerState")
    class GetState {

        @Test
        @DisplayName("returns PAYMENT_DUE when controller has paymentCutActive")
        void paymentCutActive() {
            IotController ctrl = activeController();
            ctrl.setPaymentCutActive(true);
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(ctrl));

            AreaPowerStateResponse res = service.getAreaPowerState(houseId, areaId);
            assertThat(res.powered()).isFalse();
            assertThat(res.cutReason()).isEqualTo(PowerCutReason.PAYMENT_DUE);
        }

        @Test
        @DisplayName("returns default 'power on' when no stored state")
        void defaultOn() {
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(activeController()));
            when(stateRepo.findByHouseIdAndAreaId(houseId, areaId)).thenReturn(Optional.empty());

            AreaPowerStateResponse res = service.getAreaPowerState(houseId, areaId);
            assertThat(res.powered()).isTrue();
            assertThat(res.cutReason()).isNull();
        }

        @Test
        @DisplayName("returns stored state when present")
        void stored() {
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(activeController()));
            AreaPowerState state = AreaPowerState.builder()
                    .houseId(houseId).areaId(areaId).powered(false)
                    .cutReason(PowerCutReason.MANUAL)
                    .changedAt(java.time.Instant.now()).build();
            when(stateRepo.findByHouseIdAndAreaId(houseId, areaId)).thenReturn(Optional.of(state));

            AreaPowerStateResponse res = service.getAreaPowerState(houseId, areaId);
            assertThat(res.powered()).isFalse();
            assertThat(res.cutReason()).isEqualTo(PowerCutReason.MANUAL);
        }

        @Test
        @DisplayName("throws NotFoundException when no controller")
        void noController() {
            when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAreaPowerState(houseId, areaId))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
