package com.crypto.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.crypto.service.OpenInterestService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.openInterest", havingValue = "true", matchIfMissing = false)
public class OpenInterestScheduler {
    
    private final OpenInterestService interestService;

    @PostConstruct
    public void init() {
        log.info("Initiated OpenInterestScheduler");
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void syncEveryFiveMinutes() {
        interestService.backFillOpenInterest();
    }
}
