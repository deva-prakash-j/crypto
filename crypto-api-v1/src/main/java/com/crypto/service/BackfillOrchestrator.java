package com.crypto.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.MarketType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackfillOrchestrator {

    private final OhlcvService ohlcvService;
    private final TradingPairService tradingPairService;
    private final BinanceWebClientDTO binanceWebClientDTO;
    private final OrderBookDepthService bookDepthService;


    @Value("${app.backfill.ohlcv}")
    private String backfillOhlcvFlag;

    @Value("${app.backfill.orderDepth}")
    private String backfillDepthFlag;



    public void runOhlcvInitialBackfill(MarketType type) {
        List<String> intervals = binanceWebClientDTO.getSupportedIntervals();
        List<String> supportedPairs = binanceWebClientDTO.getSupportedTokens();
        if(supportedPairs == null || supportedPairs.isEmpty()) {
            supportedPairs = tradingPairService.getTradingPairsByMarketType(type).stream()
                                .map(d -> d.getSymbol())
                                .collect(Collectors.toList());
        }
        long now = System.currentTimeMillis();

        for (String pair : supportedPairs) {
            for (String interval : intervals) {
                try {
                    log.info("Triggering backfill for {} {} {}",pair, type, interval);
                    ohlcvService.backfillOhlcv(pair, interval, type, now);
                } catch (Exception e) {
                    log.error("❌ Failed backfill for {} {} {}", pair, type, interval, e);
                }
            }
        }
    }

    public void runOrderDepth(MarketType type) {
        List<String> supportedPairs = binanceWebClientDTO.getSupportedTokens();
        if(supportedPairs == null || supportedPairs.isEmpty()) {
            supportedPairs = tradingPairService.getTradingPairsByMarketType(type).stream()
                                .map(d -> d.getSymbol())
                                .collect(Collectors.toList());
        }

        for (String pair : supportedPairs) {
            bookDepthService.syncBookDepth(pair, type);
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

        if("true".equalsIgnoreCase(backfillOhlcvFlag)) {
            for(MarketType type: supportedMarkets) {
                try {
                    runOhlcvInitialBackfill(type);
                } catch(Exception e) {
                    log.error("Failed to backfill data", e);
                }
            }
        }

        System.out.println(backfillDepthFlag);
        if("true".equalsIgnoreCase(backfillDepthFlag)) {
            log.info("Initiating Order Book Depth backfilling");
            for(MarketType type: supportedMarkets) {
                try {
                    runOrderDepth(type);
                } catch(Exception e) {
                    log.error("Failed to backfill order Depth data", e);
                }
            }
        }
    }
}
