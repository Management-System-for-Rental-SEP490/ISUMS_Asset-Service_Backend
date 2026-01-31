    package com.isums.assetservice.infrastructures.rgpc;


    import com.isums.assetservice.grpc.GetHouseRequest;
    import com.isums.assetservice.grpc.HouseResponse;
    import com.isums.assetservice.grpc.HouseServiceGrpc;
    import org.springframework.stereotype.Service;

    @Service
    public class GrpcHouseClient {

        private HouseServiceGrpc.HouseServiceBlockingStub houseStub;

        public HouseResponse getHouseById(String id){
            GetHouseRequest request = GetHouseRequest.newBuilder().setHouseId(id).build();
            return houseStub.getHouseById(request);
        }
    }
