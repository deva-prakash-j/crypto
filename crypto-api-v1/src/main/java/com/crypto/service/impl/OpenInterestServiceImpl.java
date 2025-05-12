package com.crypto.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.crypto.clients.BinanceMarketClient;
import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.MarketType;
import com.crypto.entity.OpenInterest;
import com.crypto.entity.SyncTracker;
import com.crypto.repository.OpenInterestCustomRepository;
import com.crypto.repository.SyncTrackerRepository;
import com.crypto.service.OpenInterestService;
import com.crypto.util.CommonUtil;
import com.crypto.util.Constants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenInterestServiceImpl implements OpenInterestService {

    private final BinanceWebClientDTO binanceWebClientDTO;
    private final BinanceMarketClient binanceMarketClient;
    private final SyncTrackerRepository trackerRepository;
    private final OpenInterestCustomRepository openInterestCustomRepository;

    @Override
    public void backFillOpenInterest() {
        List<MarketType> marketTypes = binanceWebClientDTO.getSupportedMarkets();
        List<String> intervals = List.of("5m","15m", "30m", "1h", "1d");
        List<String> pairs = binanceWebClientDTO.getSupportedTokens();
        Long startTime = LocalDateTime.now().minusDays(25).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        Long to = LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        for(MarketType type: marketTypes) {
            for(String pair: pairs) {
                for(String interval: intervals) {
                    syncOpenInterest(type, pair, interval, startTime, to);
                }
            }
        }
    }

    private void syncOpenInterest(MarketType type, String symbol, String interval, Long startTime, Long to) {

        SyncTracker fundingRateTracker = trackerRepository.findByMarketTypeAndSymbolAndTypeAndInterval(type.name(), symbol, Constants.OPEN_INTEREST, interval)
                .orElse(SyncTracker.builder()
                    .symbol(symbol)
                    .marketType(type)
                    .type(Constants.OPEN_INTEREST)
                    .interval(interval)
                    .lastSyncedAtUnix(null).build());

        long intervalMs = CommonUtil.getIntervalMillis(interval);
        long batchSize = intervalMs * 1000;

        long currentStart;

        if (fundingRateTracker.getLastSyncedAtUnix() != null) {
            currentStart = fundingRateTracker.getLastSyncedAtUnix() + intervalMs;
            log.info("Starting from last synced time {} for {}", fundingRateTracker.getLastSyncedAtUnix(), symbol) ;
        } else {
            currentStart = startTime;
            log.info("Detected listing time for {} {} {}: {}", symbol, type, interval, Instant.ofEpochMilli(currentStart));
        }

        while (currentStart < to) {
            long currentEnd = Math.min(currentStart + batchSize - 1, to);
            final long start = currentStart;
            final long end = currentEnd;

            List<OpenInterest> entities = binanceMarketClient
                    .getOpenInterest(symbol, type, start, interval);

            if (entities == null || entities.isEmpty()) {
                currentStart += batchSize;
                continue;
            }

            openInterestCustomRepository.bulkInsertIgnoreConflicts(entities);

            long lastOpenTime = entities.get(entities.size() - 1).getTimestamp();
            fundingRateTracker.setLastSyncedAtUnix(lastOpenTime);
            trackerRepository.save(fundingRateTracker);

            log.info("Saved {} candles for {} [{} - {}]", entities.size(), symbol,
                    Instant.ofEpochMilli(start), Instant.ofEpochMilli(end));

            currentStart += batchSize;
        }
        log.info("✅ Completed Open Interest backfill for {} [{} - {}]", symbol,
                Instant.ofEpochMilli(currentStart - batchSize), Instant.ofEpochMilli(to));
    }
    
}
