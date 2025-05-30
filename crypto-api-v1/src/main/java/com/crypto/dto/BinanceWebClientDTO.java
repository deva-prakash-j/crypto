package com.crypto.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.crypto.entity.MarketType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@ConfigurationProperties("app.binance")
@Validated
@Component
public class BinanceWebClientDTO {

    @NotBlank
    private String spotHost;

    @NotBlank
    private String futuresHost;

    @NotBlank
    private String spotMarketInfoEndpoint;

    @NotBlank
    private String futuresMarketInfoEndpoint;

    @NotBlank
    private String spotKlineEndpoint;

    @NotBlank
    private String futuresKlineEndpoint;

    @NotBlank
    private String bookDepthHost;

    @NotBlank
    private String aggTradeHost;

    @NotBlank
    private String spotAggtradeEndpoint;

    @NotBlank
    private String futuresAggtradeEndpoint;

    @NotBlank
    private String futuresFundingRateEndpoint;

    @NotBlank
    private String spotFundingRateEndpoint;

    @NotBlank
    private String futuresOpenInterestEndpoint;

    @NotBlank
    private String futuresTopLongShort;

    @NotBlank
    private String futuresGlobalLongShort;

    @NotEmpty
    private List<MarketType> supportedMarkets;

    @NotEmpty
    private List<String> supportedTokens;

    @NotEmpty
    private List<String> supportedIntervals;

}
