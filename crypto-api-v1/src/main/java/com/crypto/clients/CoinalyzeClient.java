package com.crypto.clients;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.crypto.config.CoinalyzeConfig;
import com.crypto.dto.CoinalyzeMarketInfoDTO;
import com.crypto.entity.MarketType;
import com.crypto.mapper.CoinalyzeMapper;
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

    private List<CoinalyzeMarketInfoDTO> parseAndMapMarketInfo(String rawJson) {
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(rawJson, new TypeReference<>() {});
            return mapper.mapMarketInfo(raw);
        } catch (Exception e) {
            log.error("Failed to parse Kline JSON", e);
            throw new RuntimeException("Failed to parse Kline response", e);
        }
    }


}
