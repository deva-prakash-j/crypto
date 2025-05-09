package com.crypto.scheduler;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.MarketType;
import com.crypto.service.OrderBookDepthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.orderDepth", havingValue = "true", matchIfMissing = false)
public class OrderDepthSyncScheduler {

    private final BinanceWebClientDTO binanceWebClientDTO;
    private final OrderBookDepthService bookDepthService;

    @Scheduled(cron = "0 0 2 * * *")
    public void syncEveryOneDay() {
        List<MarketType> supportedMarkets = binanceWebClientDTO.getSupportedMarkets();
        for(MarketType type: supportedMarkets) {
            try {
                bookDepthService.runOrderDepth(type);
            } catch(Exception e) {
                log.error("Failed to backfill order Depth data", e);
            }
        }
    }

    
    
}
