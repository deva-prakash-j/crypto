package com.crypto.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.crypto.service.LongShortRatioService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.longShortData", havingValue = "true", matchIfMissing = false)
public class LongShortScheduler {
    
    private final LongShortRatioService dataService;
    
    @PostConstruct
    public void init() {
        log.info("Initiated LongShortScheduler");
    }

    @Scheduled(cron = "0 */5 * * * *") // every 10 minutes
    public void syncFundingRates() {
        dataService.backFillLongShortData();
    }
}
