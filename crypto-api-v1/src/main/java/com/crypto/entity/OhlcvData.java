package com.crypto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "ohlcv_data",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"symbol", "marketType", "interval", "openTime"})
    },
    indexes = {
        @Index(name = "idx_ohlcv_symbol", columnList = "symbol"),
        @Index(name = "idx_ohlcv_market_interval", columnList = "marketType, interval"),
        @Index(name = "idx_ohlcv_time", columnList = "openTime")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OhlcvData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "market_type_enum")
    private MarketType marketType;

    @Column(nullable = false, length = 10)
    private String interval;

    @Column(nullable = false)
    private Long openTime;

    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private BigDecimal quoteVolume;
    private Long closeTime;
    private Long tradeCount;
    private BigDecimal takerBuyVolume;
    private BigDecimal takerBuyQuoteVolume;
}
