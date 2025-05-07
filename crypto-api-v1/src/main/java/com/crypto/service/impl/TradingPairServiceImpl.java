package com.crypto.service.impl;

import static com.crypto.util.Constants.REDIS_KEY_PREFIX;
import static com.crypto.util.Constants.TTL_HOURS;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import com.crypto.clients.BinanceMarketClient;
import com.crypto.clients.response.BinanceExchangeInfoResponse;
import com.crypto.dto.TradingPairDTO;
import com.crypto.entity.MarketType;
import com.crypto.entity.TradingPairMetadata;
import com.crypto.mapper.TradingPairMapper;
import com.crypto.repository.TradingPairMetadataRepository;
import com.crypto.service.TradingPairService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingPairServiceImpl implements TradingPairService {

    private final BinanceMarketClient binanceClient;
    private final TradingPairMapper mapper;
    private final TradingPairMetadataRepository repository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void syncSpotTradingPairs() {
    	binanceClient.getExchangeInfo(MarketType.SPOT)
            .map(json -> parseResponse(json, MarketType.SPOT))
            .doOnNext(this::saveToDatabase)
            .subscribe();
    }

    @Override
    public void syncFuturesTradingPairs() {
    	binanceClient.getExchangeInfo(MarketType.FUTURES_USDT)
            .map(json -> parseResponse(json, MarketType.FUTURES_USDT))
            .doOnNext(this::saveToDatabase)
            .subscribe();
    }

    private List<TradingPairDTO> parseResponse(String json, MarketType marketType) {
        try {
            BinanceExchangeInfoResponse response = objectMapper.readValue(json, BinanceExchangeInfoResponse.class);
            return mapper.mapToDTOList(response.getSymbols(), marketType);
        } catch (Exception e) {
            log.error("Failed to parse {} exchangeInfo response", marketType, e);
            throw new RuntimeException(e);
        }
    }

    private void saveToDatabase(List<TradingPairDTO> dtoList) {
    	Map<String, TradingPairMetadata> existingMap = repository.findAll().stream()
    			.collect(Collectors.toMap(
    					data -> data.getSymbol() + data.getMarketType().name(), 
    					Function.identity()));
    	TradingPairMetadata entity;
    	for (TradingPairDTO dto : dtoList) {
    		if(existingMap.containsKey(dto.getSymbol() + dto.getMarketType().name())) {
    		  entity = existingMap.get(dto.getSymbol() + dto.getMarketType().name());
    		  entity.setBaseAsset(dto.getBaseAsset());
    		  entity.setQuoteAsset(dto.getQuoteAsset());
    		  entity.setContractType(dto.getContractType());
    		  entity.setMarginAsset(dto.getMarginAsset());
    		  entity.setPricePrecision(dto.getPricePrecision());
    		  entity.setQuantityPrecision(dto.getQuantityPrecision());
    		  entity.setIsActive(dto.getIsActive());
    		} else {
    			entity = TradingPairMetadata.builder()
                        .symbol(dto.getSymbol())
                        .baseAsset(dto.getBaseAsset())
                        .quoteAsset(dto.getQuoteAsset())
                        .marketType(dto.getMarketType())
                        .contractType(dto.getContractType())
                        .marginAsset(dto.getMarginAsset())
                        .pricePrecision(dto.getPricePrecision())
                        .quantityPrecision(dto.getQuantityPrecision())
                        .isActive(dto.getIsActive())
                        .build();
    		}
    		
    		repository.save(entity);
    	}
    }
    
    @Override
    public List<TradingPairDTO> getTradingPairsByMarketType(MarketType marketType) {
        String redisKey = REDIS_KEY_PREFIX + marketType.name();
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // 1. Check Redis cache
        String cachedJson = ops.get(redisKey);
        if (cachedJson != null) {
            try {
                TradingPairDTO[] cached = objectMapper.readValue(cachedJson, TradingPairDTO[].class);
                return Arrays.asList(cached);
            } catch (Exception e) {
                log.warn("Failed to parse trading pairs from Redis for {}. Fallback to API.", marketType);
            }
        }

        // 2. Fetch from API
        List<TradingPairDTO> freshData;
        if (marketType == MarketType.SPOT || marketType == MarketType.FUTURES_USDT) {
            freshData = parseResponse(binanceClient.getExchangeInfo(marketType).block(), marketType);
        } else {
            throw new IllegalArgumentException("Unsupported MarketType: " + marketType);
        }

        
        // 3. Upsert into DB
        saveToDatabase(freshData);

        // 4. Store in Redis with 24h TTL
        try {
            String jsonToCache = objectMapper.writeValueAsString(freshData);
            ops.set(redisKey, jsonToCache, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache trading pairs in Redis for {}", marketType, e);
        }

        return freshData;
    }

    @Override
    public List<TradingPairDTO> getAllActiveTradingPairs() {
        return Stream.concat(getTradingPairsByMarketType(MarketType.SPOT).stream(), getTradingPairsByMarketType(MarketType.FUTURES_USDT).stream())
            .collect(Collectors.toList());
    }

}

