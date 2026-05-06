package com.isums.assetservice.services;

import com.isums.assetservice.infrastructures.abstracts.IotNodeTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class IotNodeTokenServiceImpl implements IotNodeTokenService {
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    @Value("${iot.node-token.secret}")
    private String secretKey;

    private byte[] signingKey;

    @PostConstruct
    void initSigningKey() {
        String configured = secretKey == null ? "" : secretKey.trim();
        if (configured.isBlank()) {
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            signingKey = generated;
            log.warn("iot.node-token.secret / IOT_NODE_TOKEN_SECRET is not configured; "
                    + "using an ephemeral runtime key for node provision tokens. "
                    + "Set IOT_NODE_TOKEN_SECRET in production.");
            return;
        }
        signingKey = configured.getBytes(StandardCharsets.UTF_8);
        if (signingKey.length < 32) {
            log.warn("iot.node-token.secret is shorter than 32 bytes; use a 32+ byte random secret in production.");
        }
    }

    public String generateToken(String serial) {
        if (serial == null || serial.isBlank()) {
            throw new IllegalArgumentException("Node serial is required");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));

            byte[] hmac = mac.doFinal(serial.trim().getBytes(StandardCharsets.UTF_8));
            String token = HexFormat.of().formatHex(hmac);

            String key = "iot-token:" + serial.trim();
            redis.opsForValue().set(key, token, TTL);

            log.info("Generated token for serial={}", serial);

            return token;
        } catch (Exception e) {
            throw new RuntimeException("Token generation failed", e);
        }
    }

    public boolean isTokenValid(String serial, String token) {
        if (serial == null || serial.isBlank() || token == null) return false;
        String key = "iot-token:" + serial.trim();
        String storedToken = redis.opsForValue().get(key);
        if (storedToken == null) return false;
        return MessageDigest.isEqual(
                storedToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    public void revokeToken(String serial) {
        String key = "iot-token:" + serial;
        redis.delete(key);
    }
}
