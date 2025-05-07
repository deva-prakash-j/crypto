package com.crypto.dto;

import com.crypto.entity.MarketType;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingPairDTO {

    private String symbol;
    private String baseAsset;
    private String quoteAsset;
    private MarketType marketType;
    private String contractType;
    private String marginAsset;
    private Integer pricePrecision;
    private Integer quantityPrecision;
    private Boolean isActive;
}