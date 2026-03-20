package com.isums.assetservice.infrastructures.abstracts;

public interface IotNodeTokenService {
    String generateToken(String serial);

    boolean isTokenValid(String serial, String token);

    void revokeToken(String serial);
}
