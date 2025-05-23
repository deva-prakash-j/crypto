package com.crypto.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.crypto.service.LiquidationDataService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.liqidationData", havingValue = "true", matchIfMissing = false)
public class LiquidityDataScheduler {

    private final LiquidationDataService dataService;
    
    @PostConstruct
    public void init() {
        log.info("Initiated LiquidityDataScheduler");
    }

    @Scheduled(cron = "0 */33 * * * *") 
    public void syncFundingRates() {
        dataService.backFillLiquidationData();
    }

}
