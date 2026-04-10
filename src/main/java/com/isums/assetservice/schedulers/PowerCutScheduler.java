package com.isums.assetservice.schedulers;

import com.isums.assetservice.domains.entities.PowerCutJob;
import com.isums.assetservice.domains.enums.PowerCutJobStatus;
import com.isums.assetservice.infrastructures.abstracts.IotProvisioningService;
import com.isums.assetservice.infrastructures.repositories.PowerCutJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PowerCutScheduler {

    private final PowerCutJobRepository powerCutJobRepo;
    private final IotProvisioningService iotProvisioningService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void executePendingPowerCuts() {
        Instant now = Instant.now();

        List<PowerCutJob> jobs = powerCutJobRepo
                .findByStatusAndExecuteAtBefore(PowerCutJobStatus.PENDING, now);

        for (PowerCutJob job : jobs) {
            try {
                iotProvisioningService.sendPowerCutCommand(job.getHouseId());

                job.setStatus(PowerCutJobStatus.EXECUTED);
                powerCutJobRepo.save(job);

                log.info("[PowerCut] Executed houseId={} contractId={}",
                        job.getHouseId(), job.getContractId());
            } catch (Exception e) {
                log.error("[PowerCut] Failed houseId={}: {}",
                        job.getHouseId(), e.getMessage(), e);
            }
        }
    }
}
