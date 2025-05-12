package com.crypto.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "long_short_ratio", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"symbol", "timestamp", "interval"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LongShortRatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    private Long timestamp; // Unix timestamp in milliseconds from Binance

    private String interval; // "5m", "15m", etc.

    private String type;

    @Column(precision = 10, scale = 6)
    private BigDecimal longAccountRatio;

    @Column(precision = 10, scale = 6)
    private BigDecimal shortAccountRatio;

    @Column(precision = 10, scale = 6)
    private BigDecimal longShortRatio;
}

