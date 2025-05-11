package com.crypto.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.crypto.dto.CoinalyzeMarketInfoDTO;

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
}
