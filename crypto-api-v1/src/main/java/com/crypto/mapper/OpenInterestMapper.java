package com.crypto.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.crypto.entity.MarketType;
import com.crypto.entity.OpenInterest;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OpenInterestMapper {
            
    public List<OpenInterest> mapToFOpenInterestEntityList(List<Map<String, Object>> rawList, MarketType type, String interval) {
        List<OpenInterest> candles = new ArrayList<>();

        for (Map<String, Object> raw : rawList) {
            try {
                candles.add(mapSingle(raw, type, interval));
            } catch (Exception e) {
                log.warn("Skipping malformed Open Interest data: {}", raw, e);
            }
        }

        return candles;
    }

    private OpenInterest mapSingle(Map<String, Object> raw, MarketType type, String interval) { 
        return OpenInterest.builder()
                .symbol(raw.get("symbol").toString())
                .marketType(type)
                .interval(interval)
                .openInterest(new BigDecimal(raw.get("sumOpenInterest").toString()))
                .openInterestValue(new BigDecimal(raw.get("sumOpenInterestValue").toString()))
                .timestamp(Long.parseLong(raw.get("timestamp").toString()))
                .build();
    }

}
