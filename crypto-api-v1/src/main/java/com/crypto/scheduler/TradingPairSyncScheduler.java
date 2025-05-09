package com.crypto.scheduler;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.MarketType;
import com.crypto.service.BackfillOrchestrator;
import com.crypto.service.TradingPairService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingPairSyncScheduler {

    private final TradingPairService tradingPairService;
    private final BackfillOrchestrator backfillOrchestrator;
    private final BinanceWebClientDTO binanceWebClientDTO;

    // Runs once every 24 hours
    @Scheduled(cron = "0 0 3 * * *") // At 03:00 AM daily
    public void refreshSpotSymbols() {
        log.info("Starting scheduled sync for SPOT trading pairs");
        try {
            tradingPairService.getTradingPairsByMarketType(MarketType.SPOT);
            log.info("Completed sync for SPOT pairs");
        } catch (Exception e) {
            log.error("Error syncing SPOT pairs", e);
        }
    }

    @Scheduled(cron = "0 10 3 * * *") // At 03:10 AM daily
    public void refreshFuturesSymbols() {
        log.info("Starting scheduled sync for FUTURES_USDT trading pairs");
        try {
            tradingPairService.getTradingPairsByMarketType(MarketType.FUTURES_USDT);
            log.info("Completed sync for FUTURES_USDT pairs");
        } catch (Exception e) {
            log.error("Error syncing FUTURES_USDT pairs", e);
        }
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeTradingPairsOnStartup() {
        log.info("Initializing trading pair metadata on application startup...");
        List<MarketType> supportedMarkets = binanceWebClientDTO.getSupportedMarkets();
        for(MarketType type: supportedMarkets) {
            try {
                tradingPairService.getTradingPairsByMarketType(type);
                log.info("{} trading pairs initialized.", type);
            } catch (Exception e) {
                log.error("Failed to initialize {} trading pairs", type, e);
            }
        }

        for(MarketType type: supportedMarkets) {
            try {
                backfillOrchestrator.runInitialBackfill(type);
            } catch(Exception e) {
                log.error("Failed to backfill data", e);
            }
        }
    }
}