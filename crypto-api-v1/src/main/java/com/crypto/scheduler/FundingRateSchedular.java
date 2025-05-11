package com.crypto.scheduler;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.MarketType;
import com.crypto.service.FundingRateService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.fundingRate", havingValue = "true", matchIfMissing = false)
public class FundingRateSchedular {
    
    private final FundingRateService fundingRateService;
    private final BinanceWebClientDTO binanceWebClientDTO;

    @PostConstruct
    public void init() {
        log.info("Initiated FundingRateSchedular");
    }

    @Scheduled(cron = "0 */10 * * * *") // every 10 minutes
    public void syncFundingRates() {
        List<MarketType> list = binanceWebClientDTO.getSupportedMarkets();

        for(MarketType type: list) {
            fundingRateService.backFillFundingRate(type);
        }
    }
}
