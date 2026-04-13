package com.isums.assetservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IotNodeTokenServiceImpl (security-critical)")
class IotNodeTokenServiceImplTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private IotNodeTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        // Inject test secret (production reads from config, no longer hardcoded)
        ReflectionTestUtils.setField(service, "secretKey", "test-secret-not-for-prod");
    }

    @Test
    @DisplayName("generateToken writes HMAC-SHA256 hex to Redis with 10-min TTL")
    void generates() {
        when(redis.opsForValue()).thenReturn(valueOps);

        String token = service.generateToken("SERIAL-123");

        assertThat(token).isNotNull().matches("^[0-9a-f]{64}$"); // 64 hex chars (SHA-256)
        verify(valueOps).set(eq("iot-token:SERIAL-123"), eq(token), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("generateToken is deterministic for same (secret, serial)")
    void deterministic() {
        when(redis.opsForValue()).thenReturn(valueOps);

        String t1 = service.generateToken("SERIAL-X");
        String t2 = service.generateToken("SERIAL-X");
        assertThat(t1).isEqualTo(t2);
    }

    @Test
    @DisplayName("isTokenValid returns true when Redis has matching token")
    void validMatch() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("iot-token:SERIAL-A")).thenReturn("abc123");

        assertThat(service.isTokenValid("SERIAL-A", "abc123")).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false when token differs")
    void invalidMismatch() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("iot-token:SERIAL-A")).thenReturn("abc123");

        assertThat(service.isTokenValid("SERIAL-A", "wrong")).isFalse();
    }

    @Test
    @DisplayName("isTokenValid returns false when Redis returns null (expired/never-generated)")
    void missingToken() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("iot-token:SERIAL-A")).thenReturn(null);

        assertThat(service.isTokenValid("SERIAL-A", "abc")).isFalse();
    }

    @Test
    @DisplayName("isTokenValid returns false when provided token null (null-safe)")
    void providedTokenNull() {
        assertThat(service.isTokenValid("SERIAL-A", null)).isFalse();
    }

    @Test
    @DisplayName("revokeToken deletes the Redis key")
    void revokes() {
        service.revokeToken("SERIAL-Z");
        verify(redis).delete("iot-token:SERIAL-Z");
    }
}
