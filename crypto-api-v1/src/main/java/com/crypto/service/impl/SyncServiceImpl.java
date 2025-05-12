package com.crypto.service.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.dto.SyncDTO;
import com.crypto.entity.FundingRate;
import com.crypto.entity.MarketType;
import com.crypto.entity.OhlcvData;
import com.crypto.entity.OpenInterest;
import com.crypto.entity.OrderBookDepth;
import com.crypto.repository.FundingRateRepository;
import com.crypto.repository.OhlcvDataRepository;
import com.crypto.repository.OpenInterestRepository;
import com.crypto.repository.OrderBookDepthRepository;
import com.crypto.service.SyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncServiceImpl implements SyncService {

    private final BinanceWebClientDTO binanceWebClientDTO;
    private final OhlcvDataRepository ohlcvDataRepository;
    private final FundingRateRepository fundingRateRepository;
    private final OpenInterestRepository openInterestRepository;
    private final OrderBookDepthRepository bookDepthRepository;

    @Override
    public Map<String, SyncDTO> getSyncStatus() {
        List<String> pairs = binanceWebClientDTO.getSupportedTokens();
        Map<String, SyncDTO> result = new HashMap<>();
        OhlcvData ohlcvData;
        FundingRate fundingRate;
        OpenInterest openInterest;
        OrderBookDepth orderBookDepth;
        for(String pair: pairs) {
            SyncDTO.SyncDTOBuilder builder = SyncDTO.builder();

            ohlcvData = ohlcvDataRepository.findByLastUpdated(MarketType.FUTURES_USDT.name(), pair, "1m").orElse(null);
            if(ohlcvData != null) {
                builder.ohlcv1mSyncedAt(Instant.ofEpochMilli(ohlcvData.getOpenTime()).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            }

            ohlcvData = ohlcvDataRepository.findByLastUpdated(MarketType.FUTURES_USDT.name(), pair, "1h").orElse(null);
            if(ohlcvData != null) {
                builder.ohlcv1hSyncedAt(Instant.ofEpochMilli(ohlcvData.getOpenTime()).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            }

            ohlcvData = ohlcvDataRepository.findByLastUpdated(MarketType.FUTURES_USDT.name(), pair, "1d").orElse(null);
            if(ohlcvData != null) {
                builder.ohlcv1dSyncedAt(Instant.ofEpochMilli(ohlcvData.getOpenTime()).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            }

            fundingRate = fundingRateRepository.findByLastUpdated(MarketType.FUTURES_USDT.name(), pair).orElse(null);
            if(fundingRate != null) {
                builder.fundingRateSyncedAt(Instant.ofEpochMilli(fundingRate.getFundingTime()).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            }

            orderBookDepth = bookDepthRepository.findByLastUpdated(MarketType.FUTURES_USDT.name(), pair).orElse(null);
            if(orderBookDepth != null) {
                builder.orderBookSyncedAt(orderBookDepth.getTimestamp().atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            }

            openInterest = openInterestRepository.findByLastUpdated(MarketType.FUTURES_USDT.name(), pair, "5m").orElse(null);
            if(openInterest != null) {
                builder.openInterest5mSyncedAt(Instant.ofEpochMilli(openInterest.getTimestamp()).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            }

            openInterest = openInterestRepository.findByLastUpdated(MarketType.FUTURES_USDT.name(), pair, "1h").orElse(null);
            if(openInterest != null) {
                builder.openInterest1hSyncedAt(Instant.ofEpochMilli(openInterest.getTimestamp()).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            }

            openInterest = openInterestRepository.findByLastUpdated(MarketType.FUTURES_USDT.name(), pair, "1d").orElse(null);
            if(openInterest != null) {
                builder.openInterest1dSyncedAt(Instant.ofEpochMilli(openInterest.getTimestamp()).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            }

            result.put(pair, builder.build());
        }
        return result;
    }
    
}
