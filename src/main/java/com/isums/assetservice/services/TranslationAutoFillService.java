package com.isums.assetservice.services;

import common.i18n.TranslationMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationAutoFillService {

    private static final List<String> SUPPORTED_LANGUAGES = List.of("vi", "en", "ja");

    private final TranslateClient translateClient;

    public TranslationMap complete(Map<String, String> input) {
        Map<String, String> normalized = normalize(input);
        if (normalized.isEmpty()) {
            throw new RuntimeException("At least one translation is required");
        }

        String sourceLanguage = resolveSourceLanguage(normalized);
        String sourceText = normalized.get(sourceLanguage);

        for (String targetLanguage : SUPPORTED_LANGUAGES) {
            if (!hasText(normalized.get(targetLanguage))) {
                normalized.put(targetLanguage, translateOrFallback(sourceText, sourceLanguage, targetLanguage));
            }
        }

        return TranslationMap.of(normalized);
    }

    private Map<String, String> normalize(Map<String, String> input) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (input == null) {
            return normalized;
        }

        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || !hasText(entry.getValue())) {
                continue;
            }
            normalized.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue().trim());
        }
        return normalized;
    }

    private String resolveSourceLanguage(Map<String, String> normalized) {
        if (hasText(normalized.get("vi"))) {
            return "vi";
        }

        return normalized.entrySet().stream()
                .filter(entry -> SUPPORTED_LANGUAGES.contains(entry.getKey()) && hasText(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseGet(() -> normalized.entrySet().stream()
                        .filter(entry -> hasText(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("At least one translation is required")));
    }

    private String translateOrFallback(String sourceText, String sourceLanguage, String targetLanguage) {
        if (targetLanguage.equalsIgnoreCase(sourceLanguage)) {
            return sourceText;
        }

        try {
            return translateClient.translateText(TranslateTextRequest.builder()
                    .text(sourceText)
                    .sourceLanguageCode(sourceLanguage)
                    .targetLanguageCode(targetLanguage)
                    .build()).translatedText();
        } catch (Exception ex) {
            log.warn("Translation fallback source={} target={}: {}", sourceLanguage, targetLanguage, ex.getMessage());
            return sourceText;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
