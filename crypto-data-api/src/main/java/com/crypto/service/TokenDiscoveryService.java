package com.crypto.service;

import com.crypto.client.CoinDeskClient;
import com.crypto.entity.ActiveToken;
import com.crypto.repository.ActiveTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import static com.crypto.util.Constant.COINDCX_TOKEN_LIST_REDIS_KEY;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenDiscoveryService {

    private static final Duration TTL = Duration.ofHours(24);

    private final CoinDeskClient coinDeskClient;
    private final ActiveTokenRepository tokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    
    public List<String> getMarketData() {
    	@SuppressWarnings("unchecked")
    	var cached = (List<String>) redisTemplate.opsForValue().get(COINDCX_TOKEN_LIST_REDIS_KEY);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
    	try {
    		String marketData = coinDeskClient.fetchMarkets();	
    		JsonNode instrumentsNode = mapper.readTree(marketData).path("Data").path("coindcx").path("instruments");
    		
    		Iterator<Map.Entry<String, JsonNode>> fields = instrumentsNode.fields();
    		
    		List<ActiveToken> activePairs = StreamSupport.stream(Spliterators.spliteratorUnknownSize(fields, 0), false)
                    .map(Map.Entry::getValue)
                    .filter(token -> "ACTIVE".equals(token.path("INSTRUMENT_STATUS").asText()))
                    .map(token -> {
                        JsonNode mapping = token.path("INSTRUMENT_MAPPING");
                        return ActiveToken.builder().
                        		pair(mapping.path("MAPPED_INSTRUMENT").asText())
                        		.baseCurrency(mapping.path("BASE").asText())
                        		.quoteCurrency(mapping.path("QUOTE").asText())
                        		.market("coindcx")
                        		.backfillDay(false)
                        		.backfillHour(false)
                        		.backfillMinute(false)
                        		.build();
                    })
                    .collect(Collectors.toList());
    		

    		for (ActiveToken entity : activePairs) {
    		    try {
    		    	tokenRepository.save(entity);
    		    } catch (Exception e) {
    		        log.warn("Skipping duplicate or invalid token: {} - {}", entity.getPair(), e.getMessage());
    		    }
    		}
            
            var pairsList =tokenRepository.findPair();

            redisTemplate.opsForValue().set(COINDCX_TOKEN_LIST_REDIS_KEY, pairsList, TTL);
            log.info("Discovered and cached {} tradable token pairs", pairsList.size());

            return pairsList;
    		
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
		return null;
    	
    }
    
}
