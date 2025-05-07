package com.crypto.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.crypto.dto.KlineCandleDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KlineMapper {

    public List<KlineCandleDTO> mapToCandleDTOList(List<List<Object>> rawList) {
        List<KlineCandleDTO> candles = new ArrayList<>();

        for (List<Object> raw : rawList) {
            try {
                candles.add(mapSingle(raw));
            } catch (Exception e) {
                log.warn("Skipping malformed kline data: {}", raw, e);
            }
        }

        return candles;
    }

    private KlineCandleDTO mapSingle(List<Object> raw) {
        return KlineCandleDTO.builder()
                .openTime(Long.parseLong(raw.get(0).toString()))
                .open(new BigDecimal(raw.get(1).toString()))
                .high(new BigDecimal(raw.get(2).toString()))
                .low(new BigDecimal(raw.get(3).toString()))
                .close(new BigDecimal(raw.get(4).toString()))
                .volume(new BigDecimal(raw.get(5).toString()))
                .closeTime(Long.parseLong(raw.get(6).toString()))
                .quoteVolume(new BigDecimal(raw.get(7).toString()))
                .tradeCount(Long.parseLong(raw.get(8).toString()))
                .takerBuyVolume(new BigDecimal(raw.get(9).toString()))
                .takerBuyQuoteVolume(new BigDecimal(raw.get(10).toString()))
                .build();
    }
}
