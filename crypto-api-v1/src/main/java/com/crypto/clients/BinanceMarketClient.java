package com.crypto.clients;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.dto.KlineCandleDTO;
import com.crypto.entity.FundingRate;
import com.crypto.entity.MarketType;
import com.crypto.mapper.FundingRateMapper;
import com.crypto.mapper.KlineMapper;
import com.crypto.util.RateLimiter;
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
    private final FundingRateMapper fundingRateMapper;

    public Mono<String> getExchangeInfo(MarketType marketType) {
        return getClient(marketType).get()
                .uri(isSpotMarket(marketType) ? binanceWebClientDTO.getSpotMarketInfoEndpoint() : binanceWebClientDTO.getFuturesMarketInfoEndpoint())
                .retrieve()
                .onStatus(status -> status.value() == 429, RateLimiter::handleRateLimiting)
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
                .onStatus(status -> status.value() == 429, RateLimiter::handleRateLimiting)
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

    public List<FundingRate> getFundingRate(String symbol, MarketType type, Long startTime) {
        return getClient(type).get()
            .uri(uriBuilder -> uriBuilder
                    .path(isSpotMarket(type) ? binanceWebClientDTO.getSpotFundingRateEndpoint() : binanceWebClientDTO.getFuturesFundingRateEndpoint())
                    .queryParam("symbol", symbol)
                    .queryParam("startTime", startTime)
                    .queryParam("limit", 1000)
                    .build())
            .retrieve()
            .onStatus(status -> status.value() == 429, RateLimiter::handleRateLimiting)
            .bodyToMono(String.class)
            .retryWhen(
                    Retry.backoff(3, Duration.ofSeconds(5))
                            .filter(ex -> ex.getMessage().contains("Rate limited"))
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure())
            )
            .map(raw -> parseFundingRate(raw, type)).block();
    }

    private List<FundingRate> parseFundingRate(String rawJson, MarketType type) {
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(rawJson, new TypeReference<>() {});
            return fundingRateMapper.mapToFundingRateEntityList(raw, type);
        } catch (Exception e) {
            log.error("Failed to parse FundingRate JSON", e);
            throw new RuntimeException("Failed to parse FundingRate response", e);
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
            .onStatus(status -> status.value() == 429, RateLimiter::handleRateLimiting)
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

    public Long findFirstAvailableFundingRate(String symbol, MarketType marketType) {
        long low = 1483228800000L; // Jan 1, 2017 UTC
        long high = System.currentTimeMillis();
        Long firstTimestamp = null;

        while (low <= high) {
            long mid = low + (high - low) / 2;

            boolean exists = fundingRateExists(symbol, mid, marketType);
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

    private boolean fundingRateExists(String symbol, long startTime, MarketType marketType) {
        try {
            String url = UriComponentsBuilder.fromPath(marketType == MarketType.FUTURES_USDT ? binanceWebClientDTO.getFuturesFundingRateEndpoint() : binanceWebClientDTO.getSpotFundingRateEndpoint())
                    .queryParam("symbol", symbol)
                    .queryParam("startTime", startTime)
                    .queryParam("limit", 1)
                    .build()
                    .toUriString();

            String response = getClient(marketType).get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<Map<String, Object>> result = new ObjectMapper().readValue(response, new TypeReference<>() {});
            log.info("Found total {} result for time between {}", result.size(), startTime);
            return !result.isEmpty();
        } catch (Exception e) {
            log.error("❌ Error checking funding rate at: " + startTime + " → " + e.getMessage());
            return false;
        }
    }

    private WebClient getClient(MarketType type) {
        return type == MarketType.SPOT ? spotWebClient : futuresWebClient;
    }

    private boolean isSpotMarket(MarketType marketType) {
        return marketType == MarketType.SPOT;
    }
}

