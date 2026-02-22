package com.isums.assetservice.configurations;

import com.isums.assetservice.grpc.HouseGrpcServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientsConfig {

    @Bean
    HouseGrpcServiceGrpc.HouseGrpcServiceBlockingStub houseStub(GrpcChannelFactory channels) {
        return HouseGrpcServiceGrpc.newBlockingStub(channels.createChannel("house-service"));
    }
}
