package com.crypto.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.crypto.clients.BinanceMarketClient;
import com.crypto.clients.CoinalyzeClient;
import com.crypto.config.CoinalyzeConfig;
import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.LiquidationData;
import com.crypto.entity.MarketType;
import com.crypto.entity.OpenInterest;
import com.crypto.entity.SyncTracker;
import com.crypto.entity.TradingPairMetadata;
import com.crypto.repository.LiquidationDataCustomRepository;
import com.crypto.repository.SyncTrackerRepository;
import com.crypto.repository.TradingPairMetadataRepository;
import com.crypto.service.LiquidationDataService;
import com.crypto.util.CommonUtil;
import com.crypto.util.Constants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class LiquidationDataServiceImpl implements LiquidationDataService {
    
    private final CoinalyzeConfig config;
    private final CoinalyzeClient client;
    private final BinanceWebClientDTO binanceWebClientDTO;
    private final TradingPairMetadataRepository metadataRepository; 
    private final SyncTrackerRepository trackerRepository;
    private final LiquidationDataCustomRepository customRepository;



    @Override
    public void backFillLiquidationData() {
       List<String> supportedIntervals = config.getSupportedIntervals();
       List<String> supportedPairs = binanceWebClientDTO.getSupportedTokens();
       Long startTime = LocalDateTime.now().minusDays(25).atZone(ZoneOffset.UTC).toInstant().getEpochSecond();
       Long to = LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant().getEpochSecond();
       for(String pair: supportedPairs) {
            for(String interval: supportedIntervals) {
                syncLiquidationData(pair, interval, startTime, to);
            }
       }
    }

    private void syncLiquidationData(String pair, String interval, Long from, Long to) {
        String symbol = getCoinalyzeSymbol(pair);
        MarketType type = MarketType.FUTURES_USDT;
        Long availableFrom = null;
        log.info("Starting Liquidation backfilling for symbol: {}", symbol);
        if(symbol != null) {
            SyncTracker fundingRateTracker = trackerRepository.findByMarketTypeAndSymbolAndTypeAndInterval(type.name(), symbol, Constants.LIQUIDATION_DATA, interval)
                .orElse(SyncTracker.builder()
                    .symbol(symbol)
                    .marketType(type)
                    .type(Constants.LIQUIDATION_DATA)
                    .interval(CommonUtil.mapInterval(interval))
                    .lastSyncedAtUnix(null).build());

            long intervalMs = CommonUtil.getIntervalMillis(CommonUtil.mapInterval(interval));
            long batchSize = intervalMs * 1000;

            long currentStart;

            if (fundingRateTracker.getLastSyncedAtUnix() != null) {
                currentStart = fundingRateTracker.getLastSyncedAtUnix() + intervalMs;
                log.info("Starting from last synced time {} for {}", fundingRateTracker.getLastSyncedAtUnix(), symbol) ;
            } else {
                Map<String, Object> queryParams = new HashMap<>();
                queryParams.put("symbols", symbol);
                queryParams.put("interval", interval);
                queryParams.put("from", from);
                queryParams.put("to", to);
                if(availableFrom == null) {
                    availableFrom = client.findFirstAvailableTime(symbol, Constants.LIQUIDATION_DATA, interval, queryParams);
                }
                currentStart = availableFrom;
                log.info("Detected listing time for {} {} {}: {}", symbol, type, interval, Instant.ofEpochSecond(currentStart));
            }

            while (currentStart < to) {
                long currentEnd = Math.min(currentStart + batchSize - 1, to);
                final long start = currentStart;
                final long end = currentEnd;

                List<LiquidationData> entities = client
                        .getLiqidityData(MarketType.FUTURES_USDT, symbol, interval, currentStart, to);

                if (entities == null || entities.isEmpty()) {
                    currentStart += batchSize;
                    continue;
                }

                customRepository.bulkInsertIgnoreConflicts(entities);

                long lastOpenTime = entities.get(entities.size() - 1).getTimestamp();
                fundingRateTracker.setLastSyncedAtUnix(lastOpenTime);
                trackerRepository.save(fundingRateTracker);

                log.info("Saved {} candles for {} {} [{} - {}]", entities.size(), interval, symbol,
                        Instant.ofEpochSecond(start), Instant.ofEpochSecond(end));

                currentStart += batchSize;
            }
            log.info("✅ Completed Liquidation Data backfill for {} {} [{} - {}]", interval, symbol,
                    Instant.ofEpochSecond(currentStart - batchSize), Instant.ofEpochSecond(to));
        }
    }

    private String getCoinalyzeSymbol(String symbol) {
        Optional<TradingPairMetadata> metadata = metadataRepository.findBySymbolAndMarketTypeCast(symbol, MarketType.FUTURES_USDT.name());
        if(metadata.isPresent()) {
            return metadata.get().getCoinalyzeSymbol();
        }
        return null;
    }
    
}
