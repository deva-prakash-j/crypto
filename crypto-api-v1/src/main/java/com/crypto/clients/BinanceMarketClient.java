package com.crypto.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.crypto.dto.KlineCandleDTO;
import com.crypto.entity.MarketType;
import com.crypto.mapper.KlineMapper;
import com.crypto.util.BinanceRateLimiter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.util.retry.Retry;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceMarketClient {

    private final WebClient spotWebClient;
    private final WebClient futuresWebClient;
    private final KlineMapper klineMapper;
    private final ObjectMapper objectMapper;

    public Mono<String> getExchangeInfo(MarketType marketType) {
        return getClient(marketType).get()
                .uri("/api/v3/exchangeInfo")
                .retrieve()
                .onStatus(status -> status.value() == 429, BinanceRateLimiter::handleRateLimiting)
                .bodyToMono(String.class)
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(5))
                                .filter(ex -> ex.getMessage().contains("Rate limited"))
                                .onRetryExhaustedThrow((spec, signal) -> signal.failure())
                );
    }

    public Mono<List<KlineCandleDTO>> getKlines(String symbol, String interval, long startTime, long endTime, MarketType marketType) {
        return getClient(marketType).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("startTime", startTime)
                        .queryParam("endTime", endTime)
                        .queryParam("limit", 1000)
                        .build())
                .retrieve()
                .onStatus(status -> status.value() == 429, BinanceRateLimiter::handleRateLimiting)
                .bodyToMono(String.class)
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(5))
                                .filter(ex -> ex.getMessage().contains("Rate limited"))
                                .onRetryExhaustedThrow((spec, signal) -> signal.failure())
                )
                .map(this::parseAndMapKlines);
    }

    private List<KlineCandleDTO> parseAndMapKlines(String rawJson) {
        try {
            List<List<Object>> raw = objectMapper.readValue(rawJson, new TypeReference<>() {});
            return klineMapper.mapToCandleDTOList(raw);
        } catch (Exception e) {
            log.error("Failed to parse Kline JSON", e);
            throw new RuntimeException("Failed to parse Kline response", e);
        }
    }

    private WebClient getClient(MarketType type) {
        return type == MarketType.SPOT ? spotWebClient : futuresWebClient;
    }
}

