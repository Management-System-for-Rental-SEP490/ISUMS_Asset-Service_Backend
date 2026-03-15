package com.isums.assetservice.infrastructures.grpcs;

import com.isums.houseservice.grpc.GetHouseRequest;
import com.isums.houseservice.grpc.HouseResponse;
import com.isums.houseservice.grpc.HouseServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseGrpcImpl extends HouseServiceGrpc.HouseServiceImplBase {
    private final HouseServiceGrpc.HouseServiceBlockingStub houseStub;

    public HouseResponse getHouseById(UUID houseId) {
        GetHouseRequest request = GetHouseRequest.newBuilder().setHouseId(houseId.toString()).build();
        return houseStub.getHouseById(request);
    }
}
