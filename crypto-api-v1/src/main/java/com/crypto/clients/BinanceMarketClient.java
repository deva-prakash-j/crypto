package com.crypto.clients;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.crypto.dto.BinanceWebClientDTO;
import com.crypto.dto.KlineCandleDTO;
import com.crypto.entity.FundingRate;
import com.crypto.entity.LongShortRatio;
import com.crypto.entity.MarketType;
import com.crypto.entity.OpenInterest;
import com.crypto.mapper.FundingRateMapper;
import com.crypto.mapper.KlineMapper;
import com.crypto.mapper.LongShortRatioMapper;
import com.crypto.mapper.OpenInterestMapper;
import com.crypto.util.RateLimiter;
import com.crypto.util.CommonUtil;
import com.crypto.util.Constants;
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
    private final OpenInterestMapper openInterestMapper;
    private final LongShortRatioMapper longShortRatioMapper;

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

    public List<OpenInterest> getOpenInterest(String symbol, MarketType type, Long startTime, String interval) {
        return getClient(type).get()
            .uri(uriBuilder -> uriBuilder
                    .path(binanceWebClientDTO.getFuturesOpenInterestEndpoint())
                    .queryParam("symbol", symbol)
                    .queryParam("startTime", startTime)
                    .queryParam("limit", 500)
                    .queryParam("period", interval)
                    .build())
            .retrieve()
            .onStatus(status -> status.value() == 429, RateLimiter::handleRateLimiting)
            .onStatus(
                status -> status.value() == 400,
                response -> response.bodyToMono(String.class).flatMap(body -> {
                    log.error("400 Bad Request: {}", body);
                    return Mono.error(new RuntimeException("Bad Request: " + body));
                })
            )
            .bodyToMono(String.class)
            .retryWhen(
                    Retry.backoff(3, Duration.ofSeconds(5))
                            .filter(ex -> ex.getMessage().contains("Rate limited"))
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure())
            )
            .map(raw -> parseOpenInterest(raw, type, interval)).block();
    }

    private List<OpenInterest> parseOpenInterest(String rawJson, MarketType type, String interval) {
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(rawJson, new TypeReference<>() {});
            return openInterestMapper.mapToFOpenInterestEntityList(raw, type, interval);
        } catch (Exception e) {
            log.error("Failed to parse Open Interest JSON", e);
            throw new RuntimeException("Failed to parse Open Interest response", e);
        }
    }

    public List<LongShortRatio> gerLongShortRatio(String symbol, Long startTime, String interval, String type) {
        return futuresWebClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path(getEndpoint(type, null))
                    .queryParam("symbol", symbol)
                    .queryParam("startTime", startTime)
                    .queryParam("limit", 500)
                    .queryParam("period", interval)
                    .build())
            .retrieve()
            .onStatus(status -> status.value() == 429, RateLimiter::handleRateLimiting)
            .onStatus(
                status -> status.value() == 400,
                response -> response.bodyToMono(String.class).flatMap(body -> {
                    log.error("400 Bad Request: {}", body);
                    return Mono.error(new RuntimeException("Bad Request: " + body));
                })
            )
            .bodyToMono(String.class)
            .retryWhen(
                    Retry.backoff(3, Duration.ofSeconds(5))
                            .filter(ex -> ex.getMessage().contains("Rate limited"))
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure())
            )
            .map(raw -> parseLongShortRatio(raw, interval, type)).block();
    }

    private List<LongShortRatio> parseLongShortRatio(String rawJson, String interval, String type) {
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(rawJson, new TypeReference<>() {});
            return longShortRatioMapper.mapToLongShortRatioEntityList(raw, type, interval);
        } catch (Exception e) {
            log.error("Failed to parse Open Interest JSON", e);
            throw new RuntimeException("Failed to parse Open Interest response", e);
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

    public Long findFirstAvailableTime(String symbol, MarketType marketType, String requestType, String interval) {
        long low = 1483228800000L; // Jan 1, 2017 UTC
        long high = System.currentTimeMillis();
        Long firstTimestamp = null;
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("symbol", symbol);
        queryParams.put("limit", 1);

        if(interval != null) {
            if(Constants.OPEN_INTEREST.equalsIgnoreCase(requestType)) {
                queryParams.put("period", interval);
            } else {
                queryParams.put("interval", interval);
            }
        }

        while (low <= high) {
            long mid = low + (high - low) / 2;

            queryParams.put("startTime", mid);
            boolean exists = recordExists(queryParams, marketType, requestType);
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

    private boolean recordExists(Map<String, Object> queryParams, MarketType marketType, String requestType) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath(getEndpoint(requestType, marketType));
            queryParams.forEach(builder::queryParam);

            String response = getClient(marketType).get()
                    .uri(builder.build().toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<Map<String, Object>> result = new ObjectMapper().readValue(response, new TypeReference<>() {});
            log.info("Found total {} result for time between {}", result.size(), queryParams.get("startTime"));
            return !result.isEmpty();
        } catch (Exception e) {
            log.error("❌ Error checking funding rate at: " + queryParams.get("startTime") + " → " + e.getMessage());
            return false;
        }
    }

    private String getEndpoint(String requestType, MarketType marketType) {
        return switch(requestType) {
            case Constants.FUNDING_RATE -> marketType == MarketType.FUTURES_USDT ? binanceWebClientDTO.getFuturesFundingRateEndpoint() : binanceWebClientDTO.getSpotFundingRateEndpoint();
            case Constants.OPEN_INTEREST -> binanceWebClientDTO.getFuturesOpenInterestEndpoint();
            case Constants.TOP_LONG_SHORT -> binanceWebClientDTO.getFuturesTopLongShort();
            case Constants.GLOBAL_LONG_SHORT -> binanceWebClientDTO.getFuturesGlobalLongShort();
            default -> throw new IllegalArgumentException("Unsupported requestType");
        };
    }

    private WebClient getClient(MarketType type) {
        return type == MarketType.SPOT ? spotWebClient : futuresWebClient;
    }

    private boolean isSpotMarket(MarketType marketType) {
        return marketType == MarketType.SPOT;
    }
}

