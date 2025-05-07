package com.crypto.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KlineCandleDTO {

    private long openTime;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private long closeTime;
    private BigDecimal quoteVolume;
    private long tradeCount;
    private BigDecimal takerBuyVolume;
    private BigDecimal takerBuyQuoteVolume;
}