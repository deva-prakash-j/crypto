package com.crypto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.webclient.coindcx-base-url}")
    private String COINDCX_BASE_URI;
    
    @Value("${app.webclient.coindesk-base-url}")
    private String COINDESK_BASE_URI;

    @Bean
    public WebClient coinDcxWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(COINDCX_BASE_URI)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(config -> config
                            .defaultCodecs()
                            .maxInMemorySize(5 * 1024 * 1024) // 5 MB
                        )
                        .build())
                .build();
    }
    
    @Bean
    public WebClient coinDeskWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(COINDESK_BASE_URI)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }

}
