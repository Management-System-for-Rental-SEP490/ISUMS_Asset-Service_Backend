    package com.isums.assetservice.infrastructures.rgpc;


    import com.isums.assetservice.grpc.GetHouseRequest;
    import com.isums.assetservice.grpc.HouseResponse;
    import com.isums.assetservice.grpc.HouseServiceGrpc;
    import lombok.RequiredArgsConstructor;
    import org.springframework.cloud.client.discovery.DiscoveryClient;
    import org.springframework.stereotype.Service;

    @Service
    @RequiredArgsConstructor
    public class GrpcHouseClient {
        private final HouseServiceGrpc.HouseServiceBlockingStub houseStub;

        public HouseResponse getHouseById(String id){
            GetHouseRequest request = GetHouseRequest.newBuilder().setId(id).build();
            return houseStub.getHouseById(request);
        }
    }
