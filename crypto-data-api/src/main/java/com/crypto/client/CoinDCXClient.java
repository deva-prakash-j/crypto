package com.crypto.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.crypto.dto.MarketDetailDTO;

import java.util.List;
import java.util.Map;

@Component
public class CoinDCXClient {

  private final WebClient coinDcxWebClient;

  @Autowired
  CoinDCXClient(WebClient coinDcxWebClient) {
    this.coinDcxWebClient = coinDcxWebClient;
  }

  @Value("${app.webclient.coindcx-market-endpoint}")
  private String coinDcxMarketEndpoint;

  public List<MarketDetailDTO> fetchMarkets() {
    return coinDcxWebClient.get()
            .uri(coinDcxMarketEndpoint)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<MarketDetailDTO>>() {})
            .block(); // Can switch to reactive flow later
  }
  
}
