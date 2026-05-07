package com.isums.assetservice.infrastructures.grpcs;

import com.isums.houseservice.grpc.GetAllHousesRequest;
import com.isums.houseservice.grpc.GetHouseByUserRequest;
import com.isums.houseservice.grpc.GetHouseRequest;
import com.isums.houseservice.grpc.HouseResponse;
import com.isums.houseservice.grpc.HouseServiceGrpc;
import com.isums.houseservice.grpc.ListHouseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HouseGrpcImpl extends HouseServiceGrpc.HouseServiceImplBase {
    private final HouseServiceGrpc.HouseServiceBlockingStub houseStub;

    public HouseResponse getHouseById(UUID houseId) {
        GetHouseRequest request = GetHouseRequest.newBuilder().setHouseId(houseId.toString()).build();
        return houseStub.getHouseById(request);
    }

    public List<HouseResponse> listHousesByLandlord(UUID landlordUserId) {
        try {
            GetAllHousesRequest req = GetAllHousesRequest.newBuilder().build();
            ListHouseResponse res = houseStub.getAllHouses(req);
            log.debug("[HouseGrpc] listHousesByLandlord userId={} → {} houses",
                    landlordUserId, res.getHouseCount());
            return res.getHouseList();
        } catch (Exception e) {
            log.warn("[HouseGrpc] listHousesByLandlord failed userId={}: {}",
                    landlordUserId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<HouseResponse> listHousesByManager(UUID managerUserId) {
        try {
            GetHouseByUserRequest req = GetHouseByUserRequest.newBuilder()
                    .setUserId(managerUserId.toString())
                    .build();
            ListHouseResponse res = houseStub.getHousesByManagerRegion(req);
            return res.getHouseList();
        } catch (Exception e) {
            log.warn("[HouseGrpc] listHousesByManager failed userId={}: {}", managerUserId, e.getMessage());
            return Collections.emptyList();
        }
    }
}

