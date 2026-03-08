package com.isums.assetservice.infrastructures.grpcs;

import com.isums.assetservice.grpc.AssetServiceGrpc;
import com.isums.assetservice.infrastructures.mapper.AssetGrpcMapper;
import com.isums.assetservice.grpc.GetAssetItemsByHouseIdRequest;
import com.isums.assetservice.grpc.GetAssetItemsResponse;
import com.isums.assetservice.infrastructures.repositories.AssetItemRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetGrpcImpl extends AssetServiceGrpc.AssetServiceImplBase {
    private final AssetItemRepository assetItemRepository;
    private final AssetGrpcMapper assetGrpcMapper;

    @Override
    @Transactional(readOnly = true)
    public void getAssetItemsByHouseId(GetAssetItemsByHouseIdRequest request,
                                       StreamObserver<GetAssetItemsResponse> responseObserver) {
        try {
            String rawHouseId = request.getHouseId();
            if (rawHouseId.isBlank()) {
                responseObserver.onError(
                        Status.INVALID_ARGUMENT.withDescription("house_id is required").asRuntimeException()
                );
                return;
            }

            UUID houseId;
            try {
                houseId = UUID.fromString(rawHouseId.trim());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(
                        Status.INVALID_ARGUMENT.withDescription("house_id is not a valid UUID").asRuntimeException()
                );
                return;
            }

            var items = assetItemRepository.findByHouseId(houseId);

            GetAssetItemsResponse.Builder res = GetAssetItemsResponse.newBuilder();
            for (var item : items) {
                res.addAssetItems(assetGrpcMapper.toDto(item));
            }

            responseObserver.onNext(res.build());
            responseObserver.onCompleted();

        } catch (DataAccessException dae) {
            log.error("DB error in getAssetItemsByHouseId", dae); // ← thêm log
            responseObserver.onError(
                    Status.UNAVAILABLE.withDescription("Database unavailable").withCause(dae).asRuntimeException()
            );
        } catch (Exception ex) {
            log.error("Unexpected error in getAssetItemsByHouseId", ex); // ← thêm log
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").withCause(ex).asRuntimeException()
            );
        }
    }
}
