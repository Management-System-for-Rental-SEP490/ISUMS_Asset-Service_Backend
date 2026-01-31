    package com.isums.assetservice.infrastructures.rgpc;


    import com.isums.assetservice.grpc.GetHouseRequest;
    import com.isums.assetservice.grpc.HouseResponse;
    import com.isums.assetservice.grpc.HouseServiceGrpc;
    import org.springframework.stereotype.Service;

    @Service
    public class GrpcHouseClient {

<<<<<<< Updated upstream
        @GrpcClient("house-service")
        private HouseGrpcServiceGrpc.HouseGrpcServiceBlockingStub houseStub;

        public HouseResponse getHouseById(String id){
            GetHouseRequest request = GetHouseRequest.newBuider().setHouseId(id).build();
=======
        private HouseServiceGrpc.HouseServiceBlockingStub houseStub;

        public HouseResponse getHouseById(String id){
            GetHouseRequest request = GetHouseRequest.newBuilder().setHouseId(id).build();
>>>>>>> Stashed changes
            return houseStub.getHouseById(request);
        }
    }
