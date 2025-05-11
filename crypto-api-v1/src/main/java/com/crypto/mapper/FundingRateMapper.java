package com.crypto.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.crypto.entity.FundingRate;
import com.crypto.entity.MarketType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FundingRateMapper {
    
        public List<FundingRate> mapToFundingRateEntityList(List<Map<String, Object>> rawList, MarketType type) {
        List<FundingRate> candles = new ArrayList<>();

        for (Map<String, Object> raw : rawList) {
            try {
                candles.add(mapSingle(raw, type));
            } catch (Exception e) {
                log.warn("Skipping malformed funding rate data: {}", raw, e);
            }
        }

        return candles;
    }

    private FundingRate mapSingle(Map<String, Object> raw, MarketType type) {
        String markPrice = raw.get("markPrice").toString();
        return FundingRate.builder()
                .symbol(raw.get("symbol").toString())
                .fundingRate(new BigDecimal(raw.get("fundingRate").toString()))
                .fundingTime(Long.parseLong(raw.get("fundingTime").toString()))
                .markPrice(new BigDecimal(markPrice != null && !markPrice.isEmpty() ? markPrice : "0"))
                .marketType(type)
                .build();
    }

}
