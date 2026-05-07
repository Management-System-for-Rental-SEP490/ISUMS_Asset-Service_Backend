package com.isums.assetservice.services;

import common.i18n.TranslationMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TranslationAutoFillService")
class TranslationAutoFillServiceTest {

    @Test
    @DisplayName("fills missing EN and JA from Vietnamese input")
    void fillsMissingLanguages() {
        TranslateClient client = mock(TranslateClient.class);
        when(client.translateText(any(TranslateTextRequest.class))).thenAnswer(invocation -> {
            TranslateTextRequest request = invocation.getArgument(0);
            return switch (request.targetLanguageCode()) {
                case "en" -> TranslateTextResponse.builder().translatedText("Air conditioner").build();
                case "ja" -> TranslateTextResponse.builder().translatedText("エアコン").build();
                default -> throw new IllegalStateException("Unexpected target");
            };
        });

        TranslationAutoFillService service = new TranslationAutoFillService(client);

        TranslationMap result = service.complete(Map.of("vi", "Máy lạnh"));

        assertThat(result.getTranslations())
                .containsEntry("vi", "Máy lạnh")
                .containsEntry("en", "Air conditioner")
                .containsEntry("ja", "エアコン");
    }

    @Test
    @DisplayName("falls back to source text when translation call fails")
    void fallsBackToSourceText() {
        TranslateClient client = mock(TranslateClient.class);
        when(client.translateText(any(TranslateTextRequest.class))).thenThrow(new RuntimeException("aws down"));

        TranslationAutoFillService service = new TranslationAutoFillService(client);

        TranslationMap result = service.complete(Map.of("vi", "Bếp từ"));

        assertThat(result.getTranslations())
                .containsEntry("vi", "Bếp từ")
                .containsEntry("en", "Bếp từ")
                .containsEntry("ja", "Bếp từ");
    }
}
