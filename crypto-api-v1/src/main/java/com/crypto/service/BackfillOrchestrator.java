package com.crypto.service;

import java.util.List;

import org.springframework.stereotype.Component;

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

    public void runInitialBackfill() {
        List<String> intervals = List.of("1d", "1h", "1m");
        List<TradingPairDTO> tradingPairs = tradingPairService.getTradingPairsByMarketType(MarketType.SPOT);
        long now = 1746599400000L;

        for (TradingPairDTO pair : tradingPairs) {
            for (String interval : intervals) {
                try {
                    log.info("Triggering backfill for {} {} {}", pair.getSymbol(), pair.getMarketType(), interval);
                    ohlcvService.backfillOhlcv(pair.getSymbol(), interval, pair.getMarketType(), now);
                } catch (Exception e) {
                    log.error("❌ Failed backfill for {} {} {}", pair.getSymbol(), pair.getMarketType(), interval, e);
                }
            }
        }
    }
}
