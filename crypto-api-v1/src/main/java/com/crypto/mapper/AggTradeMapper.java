package com.crypto.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.crypto.entity.AggTrade;
import com.crypto.entity.MarketType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AggTradeMapper {

        public List<AggTrade> mapToAggTradeEntityList(List<Map<String, Object>> rawList, String symbol, MarketType type) {
        List<AggTrade> candles = new ArrayList<>();

        for (Map<String, Object> raw : rawList) {
            try {
                candles.add(mapSingle(raw, symbol, type));
            } catch (Exception e) {
                log.warn("Skipping malformed kline data: {}", raw, e);
            }
        }

        return candles;
    }

    private AggTrade mapSingle(Map<String, Object> raw, String symbol, MarketType type) {
        return AggTrade.builder()
                .aggregateTradeId(Long.parseLong(raw.get("a").toString()))
                .price(new BigDecimal(raw.get("p").toString()))
                .quantity(new BigDecimal(raw.get("q").toString()))
                .firstTradeId(Long.parseLong(raw.get("f").toString()))
                .lastTradeId(Long.parseLong(raw.get("l").toString()))
                .timestamp(Long.parseLong(raw.get("T").toString()))
                .isBuyerMaker(Boolean.parseBoolean(raw.get("m").toString()))
                .symbol(symbol)
                .marketType(type)
                .build();
    }
    
}
