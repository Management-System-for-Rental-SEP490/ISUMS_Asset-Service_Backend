package com.isums.assetservice.infrastructures.listeners;

import com.isums.assetservice.domains.events.AssetConditionEvent;
import com.isums.assetservice.infrastructures.abstracts.AssetItemService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetConditionEventListener")
class AssetConditionEventListenerTest {

    @Mock private AssetItemService assetItemService;
    @Mock private ObjectMapper objectMapper;
    @Mock private Acknowledgment ack;

    @InjectMocks private AssetConditionEventListener listener;

    private final ConsumerRecord<String, String> rec =
            new ConsumerRecord<>("asset-condition-update-topic", 0, 0L, "k", "v");

    @Test
    @DisplayName("updates condition and acks on happy path")
    void happy() throws Exception {
        UUID assetId = UUID.randomUUID();
        AssetConditionEvent event = AssetConditionEvent.builder()
                .assetId(assetId).conditionScore(75).build();
        when(objectMapper.readValue("v", AssetConditionEvent.class)).thenReturn(event);

        listener.handleAssetConditionUpdate(rec, ack);

        verify(assetItemService).updateCondition(assetId, 75);
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("acks and swallows JacksonException (poison pill)")
    void jackson() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(AssetConditionEvent.class)))
                .thenThrow(new JacksonException("bad") {});

        listener.handleAssetConditionUpdate(rec, ack);

        verify(ack).acknowledge();
        verifyNoInteractions(assetItemService);
    }

    @Test
    @DisplayName("rethrows RuntimeException when downstream fails (Kafka retry)")
    void retry() throws Exception {
        UUID assetId = UUID.randomUUID();
        AssetConditionEvent event = AssetConditionEvent.builder().assetId(assetId).conditionScore(50).build();
        when(objectMapper.readValue("v", AssetConditionEvent.class)).thenReturn(event);
        doThrow(new RuntimeException("db")).when(assetItemService).updateCondition(any(), any());

        assertThatThrownBy(() -> listener.handleAssetConditionUpdate(rec, ack))
                .isInstanceOf(RuntimeException.class);
        verify(ack, never()).acknowledge();
    }
}
