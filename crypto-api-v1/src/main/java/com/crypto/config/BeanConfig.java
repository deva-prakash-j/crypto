package com.crypto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;

import static com.crypto.util.Constants.ACCEPT;
import static com.crypto.util.Constants.APPLICATION_JSON;
import static com.crypto.util.Constants.API_KEY;;

@Configuration
@RequiredArgsConstructor
public class BeanConfig {
	
	@Value("${app.binance.spot-host}")
	private String spotHost;
	
	@Value("${app.binance.futures-host}")
	private String futuresHost;

    private final CoinalyzeConfig coinalyzeConfig;
	
	private static final int MAX_BUFFER_SIZE = 20 * 1024 * 1024; // 5 MB

    @Bean
    WebClient spotWebClient() {
        return WebClient.builder()
                .baseUrl(spotHost)
                .defaultHeader(ACCEPT, APPLICATION_JSON)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(MAX_BUFFER_SIZE))
                        .build())
                .build();
    }

    @Bean
    WebClient futuresWebClient() {
        return WebClient.builder()
                .baseUrl(futuresHost)
                .defaultHeader(ACCEPT, APPLICATION_JSON)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(MAX_BUFFER_SIZE))
                        .build())
                .build();
    }

    @Bean
    WebClient coinalyzeWebClient() {
        return WebClient.builder()
                .baseUrl(coinalyzeConfig.getHost())
                .defaultHeader(ACCEPT, APPLICATION_JSON)
                .defaultHeader(API_KEY, coinalyzeConfig.getApiKey())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(MAX_BUFFER_SIZE))
                        .build())
                .build();
    }
    
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use string serializer for both key and value
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}