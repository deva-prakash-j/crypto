package com.crypto.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.crypto.clients.BinanceMarketClient;
import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.LongShortRatio;
import com.crypto.entity.MarketType;
import com.crypto.entity.SyncTracker;
import com.crypto.entity.TradingPairMetadata;
import com.crypto.repository.LongShortRatioCustomRepository;
import com.crypto.repository.SyncTrackerRepository;
import com.crypto.repository.TradingPairMetadataRepository;
import com.crypto.service.LongShortRatioService;
import com.crypto.util.CommonUtil;
import com.crypto.util.Constants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class LongShortRatioServiceImpl implements LongShortRatioService{
    
    private final BinanceWebClientDTO binanceWebClientDTO;
    private final TradingPairMetadataRepository metadataRepository;
    private final SyncTrackerRepository trackerRepository;
    private final BinanceMarketClient binanceMarketClient;
    private final LongShortRatioCustomRepository customRepository;

    private final List<String> types = List.of(Constants.TOP_LONG_SHORT, Constants.GLOBAL_LONG_SHORT);

    public void backFillLongShortData() {
        List<String> supportedIntervals = binanceWebClientDTO.getSupportedIntervals();
        List<TradingPairMetadata> metaDataList = metadataRepository.findByMarketTypeAndIsActiveTrue(MarketType.FUTURES_USDT.name());
        Long startTime = LocalDateTime.now().minusDays(28).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        Long to = LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        for(String type: types) {
            for(TradingPairMetadata pair: metaDataList) {
                for(String interval: supportedIntervals) {
                    if ("1m".equalsIgnoreCase(interval)) continue;
                    syncLongShortData(pair.getSymbol(), interval, startTime, to, type);
                }
            }
        }
        
    }

    private void syncLongShortData(String symbol, String interval, Long startTime, Long to, String type) {
        SyncTracker fundingRateTracker = trackerRepository.findByMarketTypeAndSymbolAndTypeAndInterval(MarketType.FUTURES_USDT.name(), symbol, type, interval)
                .orElse(SyncTracker.builder()
                    .symbol(symbol)
                    .marketType(MarketType.FUTURES_USDT)
                    .type(type)
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

            List<LongShortRatio> entities = binanceMarketClient
                    .gerLongShortRatio(symbol, start, interval, type);

            if (entities == null || entities.isEmpty()) {
                currentStart += batchSize;
                continue;
            }

            customRepository.bulkInsertIgnoreConflicts(entities);

            long lastOpenTime = entities.get(entities.size() - 1).getTimestamp();
            fundingRateTracker.setLastSyncedAtUnix(lastOpenTime);
            trackerRepository.save(fundingRateTracker);

            log.info("Saved {} candles for {} {} {} [{} - {}]", entities.size(), type, interval, symbol,
                    Instant.ofEpochMilli(start), Instant.ofEpochMilli(end));

            currentStart += batchSize;
        }
        log.info("✅ Completed Long Short Ratio backfill for {} {} {} [{} - {}]", type, interval, symbol,
                Instant.ofEpochMilli(currentStart - batchSize), Instant.ofEpochMilli(to));
    }
    
}
