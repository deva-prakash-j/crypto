package com.crypto.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.crypto.dto.CandleDTO;
import com.crypto.dto.MarketDetailDTO;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoinDeskClient {

    private final WebClient coinDeskWebClient;
    
    @Value("${app.webclient.coindesk-market-endpoint}")
    private String coinDeskMarketEndpoint;

    public String fetchMarkets() {
      return coinDeskWebClient.get()
              .uri(uriBuilder -> uriBuilder
            		  .path(coinDeskMarketEndpoint)
            		  .queryParam("instrument_status", "ACTIVE")
            		  .queryParam("market", "coindcx")
            		  .build())
              .retrieve()
              .bodyToMono(String.class)
              .block(); // Can switch to reactive flow later
    }
    
    public String fetchCandles(String instrument, String interval, long toTs, int limit) {
        String path = switch (interval) {
            case "1d" -> "/spot/v1/historical/days";
            case "1h" -> "/spot/v1/historical/hours";
            case "1m" -> "/spot/v1/historical/minutes";
            default -> throw new IllegalArgumentException("Invalid interval: " + interval);
        };

        try {
            return coinDeskWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("market", "coindcx")
                            .queryParam("instrument", instrument) // e.g., BTC-USD
                            .queryParam("limit", limit)
                            .queryParam("aggregate", 1)
                            .queryParam("to_ts", toTs)
                            .queryParam("fill", true)
                            .queryParam("apply_mapping", true)
                            .queryParam("response_format", "JSON")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch OHLCV: instrument: {},  interval: {}, limit {} , toTs {} - {}", instrument, interval, limit, toTs,e.getMessage());
            return null;
        }
    }
}
