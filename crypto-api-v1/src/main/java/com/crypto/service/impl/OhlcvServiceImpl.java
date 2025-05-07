package com.crypto.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.crypto.clients.BinanceMarketClient;
import com.crypto.dto.KlineCandleDTO;
import com.crypto.entity.MarketType;
import com.crypto.entity.OhlcvBackfillTracker;
import com.crypto.entity.OhlcvData;
import com.crypto.repository.OhlcvBackfillTrackerRepository;
import com.crypto.repository.OhlcvDataRepositoryCustom;
import com.crypto.service.OhlcvService;
import com.crypto.util.CommonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OhlcvServiceImpl implements OhlcvService {

    private final BinanceMarketClient binanceMarketClient;
    private final OhlcvDataRepositoryCustom ohlcvDataRepository;
    private final OhlcvBackfillTrackerRepository trackerRepository;

    @Override
    public void backfillOhlcv(String symbol, String interval, MarketType marketType, long to) {
        OhlcvBackfillTracker tracker = trackerRepository.findBySymbolAndMarketTypeCastAndInterval(symbol, marketType.name(), interval)
                .orElse(OhlcvBackfillTracker.builder()
                        .symbol(symbol)
                        .marketType(marketType)
                        .interval(interval)
                        .status("PENDING")
                        .lastSyncedAt(null)
                        .build());

        long intervalMs = CommonUtil.getIntervalMillis(interval);
        long batchSize = intervalMs * 1000;

        long currentStart;

        if (tracker.getLastSyncedAt() != null) {
            currentStart = tracker.getLastSyncedAt() + intervalMs;
        } else {
            currentStart = binanceMarketClient
                    .findFirstAvailableKlineTime(symbol, interval, marketType)
                    .block();
            log.info("Detected listing time for {} {} {}: {}", symbol, marketType, interval, Instant.ofEpochMilli(currentStart));
        }

        while (currentStart < to) {
            long currentEnd = Math.min(currentStart + batchSize - 1, to);
            final long start = currentStart;
            final long end = currentEnd;

            List<KlineCandleDTO> candles = binanceMarketClient
                    .getKlines(symbol, interval, start, end, marketType)
                    .onErrorResume(ex -> {
                        log.warn("Failed to fetch klines for {} from {} to {}. Skipping...", symbol, start, end, ex);
                        return Mono.just(List.of());
                    })
                    .block();

            if (candles == null || candles.isEmpty()) {
                currentStart += batchSize;
                continue;
            }

            List<OhlcvData> entities = candles.stream()
                    .map(c -> OhlcvData.builder()
                            .symbol(symbol)
                            .marketType(marketType)
                            .interval(interval)
                            .openTime(c.getOpenTime())
                            .open(c.getOpen())
                            .high(c.getHigh())
                            .low(c.getLow())
                            .close(c.getClose())
                            .volume(c.getVolume())
                            .quoteVolume(c.getQuoteVolume())
                            .closeTime(c.getCloseTime())
                            .tradeCount(c.getTradeCount())
                            .takerBuyVolume(c.getTakerBuyVolume())
                            .takerBuyQuoteVolume(c.getTakerBuyQuoteVolume())
                            .build())
                    .toList();

            ohlcvDataRepository.bulkInsertIgnoreConflicts(entities);

            long lastOpenTime = candles.get(candles.size() - 1).getOpenTime();
            tracker.setLastSyncedAt(lastOpenTime);
            tracker.setStatus("IN_PROGRESS");
            trackerRepository.save(tracker);

            log.info("Saved {} candles for {} [{} - {}]", entities.size(), symbol,
                    Instant.ofEpochMilli(start), Instant.ofEpochMilli(end));

            currentStart += batchSize;
        }

        tracker.setStatus("COMPLETED");
        trackerRepository.save(tracker);
        log.info("✅ Completed OHLCV backfill for {} [{} - {}]", symbol,
                Instant.ofEpochMilli(currentStart - batchSize), Instant.ofEpochMilli(to));
    }

}