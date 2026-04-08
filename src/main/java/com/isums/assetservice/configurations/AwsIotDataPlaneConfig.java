package com.isums.assetservice.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;

import java.net.URI;

@Configuration
public class AwsIotDataPlaneConfig {

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${app.iot.mqtt-endpoint}")
    private String mqttEndpoint;

    @Bean
    public IotDataPlaneClient iotDataPlaneClient() {
        return IotDataPlaneClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .endpointOverride(URI.create("https://" + mqttEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }
}