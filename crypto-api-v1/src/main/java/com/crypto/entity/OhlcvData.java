package com.crypto.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Column(columnDefinition = "market_type_enum")
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
