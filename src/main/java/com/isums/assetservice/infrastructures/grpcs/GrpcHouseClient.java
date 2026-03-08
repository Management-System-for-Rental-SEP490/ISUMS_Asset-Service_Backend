    package com.isums.assetservice.infrastructures.grpcs;


    import com.isums.houseservice.grpc.GetHouseRequest;
    import com.isums.houseservice.grpc.HouseResponse;
    import com.isums.houseservice.grpc.HouseServiceGrpc;
    import org.springframework.stereotype.Service;

    @Service
    public class GrpcHouseClient {

        private HouseServiceGrpc.HouseServiceBlockingStub houseStub;

        public HouseResponse getHouseById(String id){
            GetHouseRequest request = GetHouseRequest.newBuilder().setHouseId(id).build();
            return houseStub.getHouseById(request);
        }
    }
