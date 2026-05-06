package com.isums.assetservice.services;

import com.isums.assetservice.domains.entities.AssetItem;
import com.isums.assetservice.domains.entities.IoTDevice;
import com.isums.assetservice.domains.enums.NodeCapability;
import com.isums.assetservice.infrastructures.abstracts.IotNodeTokenService;
import com.isums.assetservice.infrastructures.grpcs.HouseGrpcImpl;
import com.isums.assetservice.infrastructures.repositories.AssetCategoryRepository;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import com.isums.assetservice.infrastructures.repositories.IoTDeviceRepository;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import com.isums.houseservice.grpc.FunctionalAreaResponse;
import com.isums.houseservice.grpc.HouseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IotProvisioningServiceImpl")
class IotProvisioningServiceImplTest {

    @Mock private IotClient iotClient;
    @Mock private IotControllerRepository controllerRepository;
    @Mock private DynamoDbClient dynamoDbClient;
    @Mock private IoTDeviceRepository ioTDeviceRepository;
    @Mock private AssetItemRepository assetItemRepository;
    @Mock private IoTDeviceServiceImpl ioTDeviceService;
    @Mock private IotNodeTokenService iotNodeTokenService;
    @Mock private AssetCategoryRepository assetCategoryRepository;
    @Mock private HouseGrpcImpl houseGrpc;
    @Mock private ObjectMapper objectMapper;
    @Mock private S3Presigner s3Presigner;
    @Mock private IotDataPlaneClient iotDataPlaneClient;

    @InjectMocks private IotProvisioningServiceImpl service;

    @Test
    @DisplayName("assignNodeToArea rejects foreign area")
    void assignNodeToAreaRejectsForeignArea() {
        UUID houseId = UUID.randomUUID();
        UUID validAreaId = UUID.randomUUID();
        UUID foreignAreaId = UUID.randomUUID();
        AssetItem asset = AssetItem.builder()
                .houseId(houseId)
                .build();
        IoTDevice device = IoTDevice.builder()
                .thing("node-1")
                .assetItem(asset)
                .build();

        when(ioTDeviceRepository.findByThing("node-1")).thenReturn(Optional.of(device));
        when(houseGrpc.getHouseById(houseId)).thenReturn(houseResponse(houseId, validAreaId));

        assertThatThrownBy(() -> service.assignNodeToArea("node-1", foreignAreaId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(foreignAreaId.toString())
                .hasMessageContaining(houseId.toString());

        verify(assetItemRepository, never()).save(any(AssetItem.class));
    }

    @Test
    @DisplayName("provisionNode rejects foreign area")
    void provisionNodeRejectsForeignArea() {
        UUID houseId = UUID.randomUUID();
        UUID validAreaId = UUID.randomUUID();
        UUID foreignAreaId = UUID.randomUUID();

        when(iotNodeTokenService.isTokenValid("SERIAL-1", "token-1")).thenReturn(true);
        when(houseGrpc.getHouseById(houseId)).thenReturn(houseResponse(houseId, validAreaId));

        assertThatThrownBy(() -> service.provisionNode(
                houseId,
                "SERIAL-1",
                "token-1",
                foreignAreaId,
                Set.of(NodeCapability.PZEM)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(foreignAreaId.toString())
                .hasMessageContaining(houseId.toString());

        verify(ioTDeviceRepository, never()).findBySerialNumber(any());
        verify(assetItemRepository, never()).save(any(AssetItem.class));
    }

    @Test
    @DisplayName("provisionController rejects foreign area")
    void provisionControllerRejectsForeignArea() {
        UUID houseId = UUID.randomUUID();
        UUID validAreaId = UUID.randomUUID();
        UUID foreignAreaId = UUID.randomUUID();

        when(controllerRepository.findByDeviceId("AA:BB:CC")).thenReturn(Optional.empty());
        when(houseGrpc.getHouseById(houseId)).thenReturn(houseResponse(houseId, validAreaId));

        assertThatThrownBy(() -> service.provisionController(houseId, foreignAreaId, "AA:BB:CC"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(foreignAreaId.toString())
                .hasMessageContaining(houseId.toString());

        verifyNoInteractions(iotClient);
        verify(controllerRepository, never()).save(any());
    }

    private HouseResponse houseResponse(UUID houseId, UUID... areaIds) {
        HouseResponse.Builder builder = HouseResponse.newBuilder()
                .setId(houseId.toString());

        for (UUID areaId : areaIds) {
            builder.addFunctionalAreas(FunctionalAreaResponse.newBuilder()
                    .setId(areaId.toString())
                    .setName("Area " + areaId)
                    .build());
        }

        return builder.build();
    }
}
