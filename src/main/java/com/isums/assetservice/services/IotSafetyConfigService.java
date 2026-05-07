package com.isums.assetservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.assetservice.domains.dtos.CreateIotSafetyConfigRequest;
import com.isums.assetservice.domains.dtos.IotSafetyBandDto;
import com.isums.assetservice.domains.dtos.IotSafetyCapabilityGapDto;
import com.isums.assetservice.domains.dtos.IotSafetyConfigDto;
import com.isums.assetservice.domains.dtos.IotSafetyConfigVersionDto;
import com.isums.assetservice.domains.dtos.IotSafetyScoreComponentDto;
import com.isums.assetservice.domains.dtos.IotSafetySensorDto;
import com.isums.assetservice.domains.dtos.IotSafetyThresholdDto;
import com.isums.assetservice.domains.entities.IotSafetyConfigVersion;
import com.isums.assetservice.infrastructures.repositories.IotSafetyConfigVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IotSafetyConfigService {

    private static final String SAFETY_CONFIG_CACHE = "iot-safety-config";
    private static final String ACTIVE_KEY = "active";

    private final IotSafetyConfigVersionRepository repository;
    private final ObjectMapper objectMapper;

    @Cacheable(value = SAFETY_CONFIG_CACHE, key = "'" + ACTIVE_KEY + "'")
    @Transactional(readOnly = true)
    public IotSafetyConfigDto getCurrent() {
        IotSafetyConfigVersion active = repository.findActive()
                .orElseThrow(() -> new IllegalStateException(
                        "No active iot_safety_config_version. Run migration V20260507_1500."));
        return parseConfig(active);
    }

    @Transactional(readOnly = true)
    public List<IotSafetyConfigVersionDto> getHistory() {
        return repository.findHistory().stream().map(this::toDto).toList();
    }

    @Transactional
    @CacheEvict(value = SAFETY_CONFIG_CACHE, allEntries = true)
    public IotSafetyConfigVersionDto createVersion(
            UUID actorId,
            CreateIotSafetyConfigRequest request) {
        validatePayload(request.configJson());
        Instant now = Instant.now();

        repository.findActive().ifPresent(active -> {
            active.setExpiredAt(now);
            active.setExpiredBy(actorId);
            repository.save(active);
        });

        String json;
        try {
            json = objectMapper.writeValueAsString(request.configJson());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid configJson: " + ex.getMessage(), ex);
        }

        IotSafetyConfigVersion saved = repository.save(IotSafetyConfigVersion.builder()
                .id(UUID.randomUUID())
                .version(request.version())
                .configJson(json)
                .effectiveFrom(now)
                .expiredAt(null)
                .notes(request.notes())
                .createdBy(actorId)
                .createdAt(now)
                .expiredBy(null)
                .build());

        log.info("[IoT Safety Config] New version published id={} version={} by={}",
                saved.getId(), saved.getVersion(), actorId);
        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = SAFETY_CONFIG_CACHE, allEntries = true)
    public IotSafetyConfigVersionDto expireVersion(UUID id, UUID actorId) {
        IotSafetyConfigVersion v = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + id));
        if (v.getExpiredAt() != null) {
            throw new IllegalStateException("Version already expired: " + id);
        }
        v.setExpiredAt(Instant.now());
        v.setExpiredBy(actorId);
        IotSafetyConfigVersion saved = repository.save(v);
        log.info("[IoT Safety Config] Expired version id={} by={}", id, actorId);
        return toDto(saved);
    }

    private IotSafetyConfigDto parseConfig(IotSafetyConfigVersion v) {
        try {
            JsonNode root = objectMapper.readTree(v.getConfigJson());
            return new IotSafetyConfigDto(
                    asList(root.path("activeSensors"), IotSafetySensorDto.class),
                    asList(root.path("capabilityGaps"), IotSafetyCapabilityGapDto.class),
                    asList(root.path("thresholds"), IotSafetyThresholdDto.class),
                    asList(root.path("scoreComponents"), IotSafetyScoreComponentDto.class),
                    asList(root.path("bands"), IotSafetyBandDto.class),
                    root.path("disclaimer").asText(""),
                    root.path("scoreFormulaDescription").asText(""),
                    root.path("standardsApplied").asText(""),
                    v.getVersion(),
                    v.getEffectiveFrom() == null ? null : v.getEffectiveFrom().toString(),
                    v.getNotes());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to parse safety config version " + v.getId() + ": " + ex.getMessage(), ex);
        }
    }

    private <T> List<T> asList(JsonNode node, Class<T> elementType) {
        return objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
    }

    private IotSafetyConfigVersionDto toDto(IotSafetyConfigVersion v) {
        return new IotSafetyConfigVersionDto(
                v.getId(),
                v.getVersion(),
                parseConfig(v),
                v.getEffectiveFrom(),
                v.getExpiredAt(),
                v.getNotes(),
                v.getCreatedBy(),
                v.getCreatedAt(),
                v.getExpiredBy(),
                v.getExpiredAt() == null);
    }

    private void validatePayload(JsonNode root) {
        if (root == null || root.isNull() || root.isMissingNode()) {
            throw new IllegalArgumentException("configJson is required");
        }
        if (!root.path("scoreComponents").isArray()) {
            throw new IllegalArgumentException("scoreComponents[] is required");
        }
        if (!root.path("thresholds").isArray()) {
            throw new IllegalArgumentException("thresholds[] is required");
        }
        if (!root.path("bands").isArray()) {
            throw new IllegalArgumentException("bands[] is required");
        }
        double weightSum = 0;
        for (JsonNode c : root.path("scoreComponents")) {
            weightSum += c.path("weight").asDouble(0);
        }
        if (weightSum < 0.99 || weightSum > 1.01) {
            throw new IllegalArgumentException(
                    "scoreComponents weights must sum to 1.0 (got " + weightSum + ")");
        }
    }
}
