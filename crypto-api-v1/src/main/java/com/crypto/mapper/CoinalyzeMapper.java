package com.crypto.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.crypto.dto.CoinalyzeMarketInfoDTO;
import com.crypto.entity.LiquidationData;
import com.crypto.entity.MarketType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CoinalyzeMapper {
    
    public List<CoinalyzeMarketInfoDTO> mapMarketInfo(List<Map<String, Object>> rawList) {
        List<CoinalyzeMarketInfoDTO> candles = new ArrayList<>();

        for (Map<String, Object> raw : rawList) {
            try {
                CoinalyzeMarketInfoDTO dto = mapSingleMarketInfo(raw);
                if("A".equalsIgnoreCase(dto.getExchange())) {
                    candles.add(dto);
                }
            } catch (Exception e) {
                log.warn("Skipping malformed kline data: {}", raw, e);
            }
        }

        return candles;
    }

    private CoinalyzeMarketInfoDTO mapSingleMarketInfo(Map<String, Object> raw) {
        return CoinalyzeMarketInfoDTO.builder()
            .symbol(raw.get("symbol").toString())
            .exchange(raw.get("exchange").toString())
            .symbolOnExchange(raw.get("symbol_on_exchange").toString())
            .build();
    }

    @SuppressWarnings("unchecked")
    public List<LiquidationData> mapLiquidationData(Map<String, Object> rawMap, MarketType marketType, String interval, String symbol) {
        List<LiquidationData> candles = new ArrayList<>();
        List<Map<String, Object>> rawList = (List<Map<String, Object>>) rawMap.get("history");

        for (Map<String, Object> raw : rawList) {
            try {
                candles.add(mapSingleLiqidationData(raw, symbol, marketType, interval));
            } catch (Exception e) {
                log.warn("Skipping malformed Liquidation data: {}", raw, e);
            }
        }

        return candles;
    }

    private LiquidationData mapSingleLiqidationData(Map<String, Object> raw, String symbol, MarketType marketType, String interval) {
        return LiquidationData.builder()
            .symbol(symbol)
            .marketType(marketType)
            .interval(interval)
            .timestamp(Long.parseLong(raw.get("t").toString()))
            .liquidationLong(Double.parseDouble(raw.get("l").toString()))
            .liquidationShort(Double.parseDouble(raw.get("s").toString()))
            .build();
    }
}
