package com.crypto.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Component
@ConfigurationProperties("app.coinalyze")
@Data
public class CoinalyzeConfig {
    
    @NotBlank
    private String host;

    @NotBlank
    private String futureMarkets;

    @NotBlank
    private String spotMarkets;

    @NotBlank
    private String liqidationHistory;

    @NotBlank
    private String apiKey;

    @NotEmpty
    private List<String> supportedIntervals;
}
