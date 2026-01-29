    package com.isums.assetservice.infrastructures.rgpc;


    import com.isums.assetservice.grpc.GetHouseRequest;
    import com.isums.assetservice.grpc.HouseResponse;
    import org.springframework.stereotype.Service;

    @Service
    public class GrpcHouseClient {

        @GrpcClient("house-service")
        private HouseGrpcServiceGrpc.HouseGrpcServiceBlockingStub houseStub;

        public HouseResponse getHouseById(String id){
            GetHouseRequest request = GetHouseRequest.newBuider().setHouseId(id).build();
            return houseStub.getHouseById(request);
        }
    }
