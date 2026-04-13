package com.isums.assetservice.services;

import com.isums.assetservice.infrastructures.abstracts.IotNodeTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    public String generateToken(String serial) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            byte[] hmac = mac.doFinal(serial.getBytes(StandardCharsets.UTF_8));
            String token = HexFormat.of().formatHex(hmac);

            String key = "iot-token:" + serial;
            redis.opsForValue().set(key, token, TTL);

            log.info("Generated token for serial={}", serial);

            return token;
        } catch (Exception e) {
            throw new RuntimeException("Token generation failed", e);
        }
    }

    public boolean isTokenValid(String serial, String token) {
        if (token == null) return false;
        String key = "iot-token:" + serial;
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
