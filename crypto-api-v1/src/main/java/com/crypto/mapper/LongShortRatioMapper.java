package com.crypto.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.crypto.entity.LongShortRatio;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LongShortRatioMapper {
    
    public List<LongShortRatio> mapToLongShortRatioEntityList(List<Map<String, Object>> rawList, String type, String interval) {
        List<LongShortRatio> candles = new ArrayList<>();

        for (Map<String, Object> raw : rawList) {
            try {
                candles.add(mapSingle(raw, type, interval));
            } catch (Exception e) {
                log.warn("Skipping malformed Long Short Ratio data: {}", raw, e);
            }
        }

        return candles;
    }

    private LongShortRatio mapSingle(Map<String, Object> raw, String type, String interval) {
        return LongShortRatio.builder()
                .symbol(raw.get("symbol").toString())
                .type(type)
                .timestamp(Long.parseLong(raw.get("timestamp").toString()))
                .interval(interval)
                .longAccountRatio(new BigDecimal(raw.get("longAccount").toString()))
                .shortAccountRatio(new BigDecimal(raw.get("shortAccount").toString()))
                .longShortRatio(new BigDecimal(raw.get("longShortRatio").toString()))
                .build();
    }

}
