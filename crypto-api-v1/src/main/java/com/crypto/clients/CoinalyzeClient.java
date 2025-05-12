package com.crypto.clients;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.crypto.config.CoinalyzeConfig;
import com.crypto.dto.CoinalyzeMarketInfoDTO;
import com.crypto.entity.LiquidationData;
import com.crypto.entity.MarketType;
import com.crypto.mapper.CoinalyzeMapper;
import com.crypto.util.Constants;
import com.crypto.util.RateLimiter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoinalyzeClient {
    
    private final CoinalyzeConfig config;
    private final WebClient coinalyzeWebClient;
    private final ObjectMapper objectMapper;
    private final CoinalyzeMapper mapper;

    public List<CoinalyzeMarketInfoDTO> getExchangeMapping(MarketType marketType) {
        return coinalyzeWebClient.get()
            .uri(marketType == MarketType.FUTURES_USDT ? config.getFutureMarkets() : config.getSpotMarkets())
            .retrieve()
            .onStatus(status -> status.value() == 429, RateLimiter::handleRateLimiting)
            .bodyToMono(String.class)
            .retryWhen(
                Retry.backoff(3, Duration.ofSeconds(5))
                    .filter(ex -> ex.getMessage().contains("Rate limited"))
                    .onRetryExhaustedThrow((spec, signal) -> signal.failure())
            ).map(this::parseAndMapMarketInfo)
            .block();
    }

    public List<LiquidationData> getLiqidityData(MarketType marketType, String symbol, String interval, Long from, Long to, String binanceSymbol) {
        return coinalyzeWebClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path(config.getLiqidationHistory())
                    .queryParam("symbols", symbol)
                    .queryParam("interval", interval)
                    .queryParam("from", from)
                    .queryParam("to", to)
                    .build())
            .retrieve()
            .onStatus(status -> status.value() == 429, RateLimiter::handleRateLimiting)
            .bodyToMono(String.class)
            .retryWhen(
                Retry.backoff(3, Duration.ofSeconds(5))
                    .filter(ex -> ex.getMessage().contains("Rate limited"))
                    .onRetryExhaustedThrow((spec, signal) -> signal.failure())
            ).map(rawData -> parseAndMapLiquidationData(rawData, marketType, interval, binanceSymbol))
            .block();
    }

    private List<CoinalyzeMarketInfoDTO> parseAndMapMarketInfo(String rawJson) {
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(rawJson, new TypeReference<>() {});
            return mapper.mapMarketInfo(raw);
        } catch (Exception e) {
            log.error("Failed to parse Kline JSON", e);
            throw new RuntimeException("Failed to parse Kline response", e);
        }
    }

    private List<LiquidationData> parseAndMapLiquidationData(String rawJson, MarketType marketType, String interval, String symbol) {
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(rawJson, new TypeReference<>() {});
            return mapper.mapLiquidationData(raw.get(0), marketType, interval, symbol);
        } catch (Exception e) {
            log.error("Failed to parse Kline JSON", e);
            throw new RuntimeException("Failed to parse Kline response", e);
        }
    }

    
    public Long findFirstAvailableTime(String symbol, String requestType, String interval, Map<String, Object> queryParams) {
        long low = Long.parseLong(queryParams.get("from").toString()); // Jan 1, 2017 UTC
        long high = Long.parseLong(queryParams.get("to").toString());
        Long firstTimestamp = null;

        while (low <= high) {
            long mid = low + (high - low) / 2;

            queryParams.put("startTime", mid);
            boolean exists = recordExists(queryParams, requestType);
            if (exists) {
                firstTimestamp = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        log.info("First available funding rate for {} is likely around {}", symbol, firstTimestamp);
        return firstTimestamp;
    }


    private boolean recordExists(Map<String, Object> queryParams, String requestType) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath(getEndpoint(requestType));
            queryParams.forEach(builder::queryParam);

            String response = coinalyzeWebClient.get()
                    .uri(builder.build().toString())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, RateLimiter::handleRateLimiting)
                    .bodyToMono(String.class)
                    .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(5))
                        .filter(ex -> ex.getMessage().contains("Rate limited"))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .block();

            List<Map<String, Object>> result = new ObjectMapper().readValue(response, new TypeReference<>() {});
            return !result.isEmpty();
        } catch (Exception e) {
            log.error("❌ Error checking funding rate at: " + queryParams.get("startTime") + " → " + e.getMessage());
            return false;
        }
    }

    private String getEndpoint(String requestType) {
        return switch(requestType) {
            case Constants.LIQUIDATION_DATA -> config.getLiqidationHistory();
            default -> throw new IllegalArgumentException("Unsupported requestType");
        };
    }


}
