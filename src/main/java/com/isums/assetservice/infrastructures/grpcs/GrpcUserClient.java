package com.isums.assetservice.infrastructures.grpcs;

import com.isums.userservice.grpc.GetUserIdAndRoleByKeyCloakIdRequest;
import com.isums.userservice.grpc.UserResponse;
import com.isums.userservice.grpc.UserServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GrpcUserClient {
    private final UserServiceGrpc.UserServiceBlockingStub stub;

    public UserResponse getUserIdAndRoleByKeyCloakId(String keycloakId) {
        GetUserIdAndRoleByKeyCloakIdRequest req = GetUserIdAndRoleByKeyCloakIdRequest.newBuilder().setKeycloakId(keycloakId).build();
        return stub.getUserIdAndRoleByKeyCloakId(req);
    }
}
