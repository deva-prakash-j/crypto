package com.crypto.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.crypto.client.CoinDeskClient;
import com.crypto.dto.CandleDTO;
import com.crypto.entity.OhlcvData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OhlcvFetcherService {

    private final CoinDeskClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public List<CandleDTO> fetchBackfill(String instrument, String interval, long from, long to) {
        long cursor = to;
        List<CandleDTO> allCandles = new ArrayList<CandleDTO>();

        while (cursor > from) {
            String response = client.fetchCandles(instrument, interval, cursor, 2000);
            if (response.isEmpty() || response == null) break;
            List<CandleDTO> batch = mapToDto(response);
            if(batch.isEmpty() || batch == null) break;
            

            allCandles.addAll(batch);

            // Move window back
            long oldest = batch.stream().mapToLong(CandleDTO::getTimestamp).min().orElse(cursor);
            cursor = (oldest / 1000) - 1;

            if (batch.size() < 2000) break;
        }

        return allCandles;
    }

    public List<CandleDTO> fetchIncremental(String instrument, String interval, long from) {
        return fetchBackfill(instrument, interval, from, Instant.now().getEpochSecond());
    }
    
    public OhlcvData mapToEntity(CandleDTO c, String pair, String interval) {
        return OhlcvData.builder()
                .pair(pair)
                .interval(interval)
                .timestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(c.getTimestamp()), ZoneOffset.UTC))

                .open(BigDecimal.valueOf(c.getOpen()))
                .high(BigDecimal.valueOf(c.getHigh()))
                .low(BigDecimal.valueOf(c.getLow()))
                .close(BigDecimal.valueOf(c.getClose()))
                
                .firstTradeTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(c.getFirstTradeTimestamp()), ZoneOffset.UTC))
                .lastTradeTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(c.getLastTradeTimestamp()), ZoneOffset.UTC))
                .lowTradeTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(c.getLowTradeTimestamp()), ZoneOffset.UTC))
                .highTradeTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(c.getHighTradeTimestamp()), ZoneOffset.UTC))
                
                .firstTradePrice(BigDecimal.valueOf(c.getFirstTradePrice()))
                .lastTradePrice(BigDecimal.valueOf(c.getLastTradePrice()))
                .lowTradePrice(BigDecimal.valueOf(c.getLowTradePrice()))
                .highTradePrice(BigDecimal.valueOf(c.getHighTradePrice()))

                .volume(BigDecimal.valueOf(c.getVolume()))
                .quoteVolume(BigDecimal.valueOf(c.getQuoteVolume()))
                .volumeBuy(BigDecimal.valueOf(c.getVolumeBuy()))
                .quoteVolumeBuy(BigDecimal.valueOf(c.getQuoteVolumeBuy()))
                .volumeSell(BigDecimal.valueOf(c.getVolumeSell()))
                .quoteVolumeSell(BigDecimal.valueOf(c.getQuoteVolumeSell()))
                .volumeUnknown(BigDecimal.valueOf(c.getVolumeUnknown()))
                .quoteVolumeUnknown(BigDecimal.valueOf(c.getQuoteVolumeUnknown()))

                .totalTrades(c.getTotalTrades())
                .totalBuyTrades(c.getTotalTradesBuy())
                .totalSellTrades(c.getTotalTradesSell())
                .totalUnknownTrades(c.getTotalTradesUnknown())

                .lastUpdated(LocalDateTime.now())
                .build();
    }
    
    public List<CandleDTO> mapToDto(String response) {
    	try {
    	JsonNode dataNode = mapper.readTree(response).path("Data");
    	List<CandleDTO> list = mapper.readerForListOf(CandleDTO.class).readValue(dataNode);
    	return list;
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return null;
    }

}
