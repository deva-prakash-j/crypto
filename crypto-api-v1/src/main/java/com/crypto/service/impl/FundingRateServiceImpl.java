package com.crypto.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.crypto.clients.BinanceMarketClient;
import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.entity.FundingRate;
import com.crypto.entity.MarketType;
import com.crypto.entity.SyncTracker;
import com.crypto.repository.FundingRateCustomRepository;
import com.crypto.repository.SyncTrackerRepository;
import com.crypto.service.FundingRateService;
import com.crypto.util.Constants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FundingRateServiceImpl implements FundingRateService {

    private final BinanceWebClientDTO binanceWebClientDTO;
    private final BinanceMarketClient binanceMarketClient;
    private final SyncTrackerRepository trackerRepository;
    private final FundingRateCustomRepository fundingRateCustomRepository;

    @Override
    public void backFillFundingRate(MarketType marketType) {
       List<String> supportedPairs = binanceWebClientDTO.getSupportedTokens();

       for(String pair: supportedPairs) {
            syncFundingRate(pair, marketType);
       }
    }

    @Override
    public void syncFundingRate(String symbol, MarketType marketType) {
        SyncTracker fundingRateTracker = trackerRepository.findByMarketTypeAndSymbolAndType(marketType.name(), symbol, Constants.FUNDING_RATE)
                .orElse(SyncTracker.builder()
                    .symbol(symbol)
                    .marketType(marketType)
                    .type(Constants.FUNDING_RATE)
                    .lastSyncedDate(null).build());

        long currentStart;
        long to = System.currentTimeMillis();
        long yesterdayMidnightUtc = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli();


        if (fundingRateTracker.getLastSyncedDate() != null) {
            currentStart = fundingRateTracker.getLastSyncedDate().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();
            log.info("Starting from last synced time {} for {}", fundingRateTracker.getLastSyncedDate(), symbol) ;
        } else {
            currentStart = binanceMarketClient
                    .findFirstAvailableTime(symbol, marketType, Constants.FUNDING_RATE, null);
            log.info("Detected listing time for {} {}: {}", symbol, marketType, Instant.ofEpochMilli(currentStart));
        }

        while (currentStart < to) {
            final long start = currentStart;

            List<FundingRate> fundingRate = binanceMarketClient
                    .getFundingRate(symbol, marketType, start);

            if (fundingRate == null || fundingRate.isEmpty()) {
                continue;
            }

            fundingRateCustomRepository.bulkInsertIgnoreConflicts(fundingRate);

            long lastOpenTime = fundingRate.get(fundingRate.size() - 1).getFundingTime();
            fundingRateTracker.setLastSyncedDate(Instant.ofEpochMilli(currentStart).atZone(ZoneId.of("UTC")).toLocalDate());
            trackerRepository.save(fundingRateTracker);

            log.info("Saved {} candles for {} [{} - {}]", fundingRate.size(), symbol,
                    Instant.ofEpochMilli(start), Instant.ofEpochMilli(lastOpenTime));

            currentStart = lastOpenTime;

            if (lastOpenTime >= yesterdayMidnightUtc) {
                log.info("✅ Reached end of current date, stopping sync.");
                break;
            }
        }
    }

    
    
}
