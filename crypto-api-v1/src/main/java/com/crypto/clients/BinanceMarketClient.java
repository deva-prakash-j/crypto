package com.crypto.clients;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.dto.KlineCandleDTO;
import com.crypto.entity.MarketType;
import com.crypto.mapper.KlineMapper;
import com.crypto.util.BinanceRateLimiter;
import com.crypto.util.CommonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceMarketClient {

    private final WebClient spotWebClient;
    private final WebClient futuresWebClient;
    private final KlineMapper klineMapper;
    private final ObjectMapper objectMapper;
    private final BinanceWebClientDTO binanceWebClientDTO;

    public Mono<String> getExchangeInfo(MarketType marketType) {
        return getClient(marketType).get()
                .uri(isSpotMarket(marketType) ? binanceWebClientDTO.getSpotMarketInfoEndpoint() : binanceWebClientDTO.getFuturesMarketInfoEndpoint())
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
                        .path(isSpotMarket(marketType) ? binanceWebClientDTO.getSpotKlineEndpoint() : binanceWebClientDTO.getFuturesKlineEndpoint())
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

    public Mono<Long> findFirstAvailableKlineTime(String symbol, String interval, MarketType marketType) {
        long intervalMs = CommonUtil.getIntervalMillis(interval);
        long low = 1483228800000L; // Jan 1, 2017 UTC
        long high = System.currentTimeMillis();
    
        return binarySearchKline(symbol, interval, marketType, low, high, intervalMs);
    }

    private Mono<Long> binarySearchKline(String symbol, String interval, MarketType marketType,
                                     long low, long high, long intervalMs) {

    if ((high - low) <= intervalMs) {
        log.info("First available kline for {} is likely around {}", symbol, high);
        return Mono.just(high);
    }

    long mid = (low + high) / 2;

    return getClient(marketType).get()
            .uri(uriBuilder -> uriBuilder
                    .path(isSpotMarket(marketType) ? binanceWebClientDTO.getSpotKlineEndpoint() : binanceWebClientDTO.getFuturesKlineEndpoint())
                    .queryParam("symbol", symbol)
                    .queryParam("interval", interval)
                    .queryParam("startTime", mid)
                    .queryParam("limit", 1)
                    .build())
            .retrieve()
            .onStatus(status -> status.value() == 429, BinanceRateLimiter::handleRateLimiting)
            .bodyToMono(String.class)
            .retryWhen(
                    Retry.backoff(3, Duration.ofSeconds(5))
                            .filter(ex -> ex.getMessage().contains("Rate limited"))
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure())
            )
            .flatMap(json -> {
                try {
                    List<List<Object>> response = objectMapper.readValue(json, new TypeReference<>() {});
                    if (response.isEmpty()) {
                        return binarySearchKline(symbol, interval, marketType, mid + intervalMs, high, intervalMs);
                    } else {
                        return binarySearchKline(symbol, interval, marketType, low, mid, intervalMs);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse kline response during binary search", e);
                    return Mono.error(new RuntimeException("Failed during kline binary search", e));
                }
            });
    }

    private WebClient getClient(MarketType type) {
        return type == MarketType.SPOT ? spotWebClient : futuresWebClient;
    }

    private boolean isSpotMarket(MarketType marketType) {
        return marketType == MarketType.SPOT;
    }
}

