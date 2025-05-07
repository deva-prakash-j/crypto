package com.crypto.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
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

}
