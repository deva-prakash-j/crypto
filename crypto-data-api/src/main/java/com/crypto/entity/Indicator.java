package com.crypto.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "indicators",
    indexes = {@Index(name = "idx_indicators_pair_interval_time",
        columnList = "pair, interval, timestamp DESC")})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Indicator {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  @Column(nullable = false)
  private String pair;

  @Column(nullable = false)
  private String interval;

  private BigDecimal rsi;
  private BigDecimal macd;
  private BigDecimal macdSignal;
  private BigDecimal macdHistogram;
  private BigDecimal sma20;
  private BigDecimal sma50;
  private BigDecimal bollingerUpper;
  private BigDecimal bollingerLower;

  // Optional: you can link to OHLCV with @ManyToOne if needed
  // @ManyToOne(fetch = FetchType.LAZY)
  // @JoinColumns({
  // @JoinColumn(name = "timestamp", referencedColumnName = "timestamp", insertable = false,
  // updatable = false),
  // @JoinColumn(name = "pair", referencedColumnName = "pair", insertable = false, updatable =
  // false),
  // @JoinColumn(name = "interval", referencedColumnName = "interval", insertable = false, updatable
  // = false)
  // })
  // private OhlcvData ohlcvData;
}
