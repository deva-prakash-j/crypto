package com.crypto.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.dto.TradingPairDTO;
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

    public void runInitialBackfill(MarketType type) {
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
}
