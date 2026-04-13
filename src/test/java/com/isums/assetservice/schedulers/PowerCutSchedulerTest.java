package com.isums.assetservice.schedulers;

import com.isums.assetservice.domains.entities.IotController;
import com.isums.assetservice.domains.entities.PowerCutJob;
import com.isums.assetservice.domains.enums.IotControllerStatus;
import com.isums.assetservice.domains.enums.PowerCutJobStatus;
import com.isums.assetservice.infrastructures.abstracts.IotProvisioningService;
import com.isums.assetservice.infrastructures.repositories.AreaPowerStateRepository;
import com.isums.assetservice.infrastructures.repositories.IotControllerRepository;
import com.isums.assetservice.infrastructures.repositories.PowerCutJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PowerCutScheduler")
class PowerCutSchedulerTest {

    @Mock private PowerCutJobRepository jobRepo;
    @Mock private IotProvisioningService provisioning;
    @Mock private IotControllerRepository controllerRepo;
    @Mock private AreaPowerStateRepository stateRepo;

    @InjectMocks private PowerCutScheduler scheduler;

    private PowerCutJob pendingJob(UUID houseId) {
        return PowerCutJob.builder()
                .id(UUID.randomUUID()).houseId(houseId)
                .contractId(UUID.randomUUID()).executeAt(Instant.now().minusSeconds(60))
                .status(PowerCutJobStatus.PENDING).build();
    }

    @Test
    @DisplayName("executes pending job: calls IoT, marks controller cut-active, marks areas PAYMENT_DUE, persists EXECUTED")
    void executesJob() {
        UUID houseId = UUID.randomUUID();
        PowerCutJob job = pendingJob(houseId);
        IotController ctrl = IotController.builder()
                .id(UUID.randomUUID()).deviceId("d1").houseId(houseId)
                .status(IotControllerStatus.ACTIVE).paymentCutActive(false).build();

        when(jobRepo.findByStatusAndExecuteAtBefore(eq(PowerCutJobStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(job));
        when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.of(ctrl));
        when(stateRepo.markAllAreasPaymentDue(houseId, job.getId())).thenReturn(3);

        scheduler.executePendingPowerCuts();

        verify(provisioning).sendPowerCutCommand(houseId);
        assertThat(ctrl.isPaymentCutActive()).isTrue();
        assertThat(ctrl.getActivePaymentCutJobId()).isEqualTo(job.getId());
        verify(controllerRepo).save(ctrl);
        verify(stateRepo).markAllAreasPaymentDue(houseId, job.getId());
        assertThat(job.getStatus()).isEqualTo(PowerCutJobStatus.EXECUTED);
        verify(jobRepo).save(job);
    }

    @Test
    @DisplayName("continues processing next job when one fails (error isolation)")
    void isolatesFailure() {
        UUID houseA = UUID.randomUUID();
        UUID houseB = UUID.randomUUID();
        PowerCutJob bad = pendingJob(houseA);
        PowerCutJob good = pendingJob(houseB);

        when(jobRepo.findByStatusAndExecuteAtBefore(any(), any())).thenReturn(List.of(bad, good));
        doThrow(new RuntimeException("iot down"))
                .when(provisioning).sendPowerCutCommand(houseA);

        // controller for B so the good path proceeds
        IotController ctrlB = IotController.builder()
                .id(UUID.randomUUID()).deviceId("d2").houseId(houseB)
                .status(IotControllerStatus.ACTIVE).paymentCutActive(false).build();
        when(controllerRepo.findByHouseId(houseB)).thenReturn(Optional.of(ctrlB));

        scheduler.executePendingPowerCuts();

        // bad job retained PENDING (not EXECUTED)
        assertThat(bad.getStatus()).isEqualTo(PowerCutJobStatus.PENDING);
        // good job executed
        verify(provisioning).sendPowerCutCommand(houseB);
        assertThat(good.getStatus()).isEqualTo(PowerCutJobStatus.EXECUTED);
    }

    @Test
    @DisplayName("no work when no pending jobs")
    void noWork() {
        when(jobRepo.findByStatusAndExecuteAtBefore(any(), any())).thenReturn(List.of());

        scheduler.executePendingPowerCuts();

        verifyNoInteractions(provisioning, controllerRepo, stateRepo);
    }

    @Test
    @DisplayName("marks EXECUTED even when controller missing (already-deprovisioned case)")
    void missingController() {
        UUID houseId = UUID.randomUUID();
        PowerCutJob job = pendingJob(houseId);
        when(jobRepo.findByStatusAndExecuteAtBefore(any(), any())).thenReturn(List.of(job));
        when(controllerRepo.findByHouseId(houseId)).thenReturn(Optional.empty());

        scheduler.executePendingPowerCuts();

        verify(controllerRepo, never()).save(any());
        assertThat(job.getStatus()).isEqualTo(PowerCutJobStatus.EXECUTED);
    }
}
