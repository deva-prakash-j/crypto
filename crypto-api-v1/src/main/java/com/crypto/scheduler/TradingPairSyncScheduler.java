package com.crypto.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

        try {
            tradingPairService.getTradingPairsByMarketType(MarketType.SPOT);
            log.info("SPOT trading pairs initialized.");
        } catch (Exception e) {
            log.error("Failed to initialize SPOT trading pairs", e);
        }

        // try {
        //     tradingPairService.getTradingPairsByMarketType(MarketType.FUTURES_USDT);
        //     log.info("FUTURES_USDT trading pairs initialized.");
        // } catch (Exception e) {
        //     log.error("Failed to initialize FUTURES_USDT trading pairs", e);
        // }

        try {
            backfillOrchestrator.runInitialBackfill();
        } catch(Exception e) {
            log.error("Failed to backfill data", e);
        }
    }
}