    package com.isums.assetservice.infrastructures.grpcs;


    import com.isums.assetservice.grpc.GetHouseRequest;
    import com.isums.assetservice.grpc.HouseResponse;
    import com.isums.assetservice.grpc.HouseGrpcServiceGrpc;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;

    @Service
    @RequiredArgsConstructor
    public class GrpcHouseClient {

        private final HouseGrpcServiceGrpc.HouseGrpcServiceBlockingStub houseStub;

        public HouseResponse getHouseById(String id){
            GetHouseRequest request = GetHouseRequest.newBuilder().setId(id).build();
            return houseStub.getHouseById(request);
        }
    }
