    package com.isums.assetservice.infrastructures.grpcs;


    import com.isums.assetservice.grpc.GetHouseRequest;
    import com.isums.assetservice.grpc.HouseResponse;
    import com.isums.assetservice.grpc.HouseGrpcServiceGrpc;
    import org.springframework.stereotype.Service;

    @Service
    public class GrpcHouseClient {

        private HouseGrpcServiceGrpc.HouseGrpcServiceBlockingStub houseStub;

        public HouseResponse getHouseById(String id){
            GetHouseRequest request = GetHouseRequest.newBuilder().setHouseId(id).build();
            return houseStub.getHouseById(request);
        }
    }
