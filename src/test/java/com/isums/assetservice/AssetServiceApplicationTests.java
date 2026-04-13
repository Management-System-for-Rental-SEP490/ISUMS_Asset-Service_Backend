package com.isums.assetservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires Postgres/Kafka/Redis/DynamoDB/AWS IoT/Lambda/S3/gRPC infrastructure; run as integration test with Testcontainers + LocalStack")
class AssetServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
