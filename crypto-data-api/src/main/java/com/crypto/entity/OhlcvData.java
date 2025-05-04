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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ohlcv_data",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"timestamp", "pair", "interval"})},
    indexes = {
        @Index(name = "idx_ohlcv_pair_interval_time",
            columnList = "pair, interval, timestamp DESC"),
        @Index(name = "idx_ohlcv_market_pair", columnList = "market, pair")})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OhlcvData {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private String pair;

  @Column(nullable = false)
  private String interval; // '1m', '1h', '1d'

  @Column(nullable = false)
  private LocalDateTime timestamp;

  private BigDecimal open;
  private BigDecimal high;
  private BigDecimal low;
  private BigDecimal close;
  
  private LocalDateTime firstTradeTimestamp;
  private LocalDateTime lastTradeTimestamp;
  private BigDecimal firstTradePrice;
  private BigDecimal lastTradePrice;
  private BigDecimal highTradePrice;
  private LocalDateTime highTradeTimestamp;
  private BigDecimal lowTradePrice;
  private LocalDateTime lowTradeTimestamp;

  private BigDecimal volume;
  private BigDecimal quoteVolume;
  
  private BigDecimal volumeBuy;
  private BigDecimal quoteVolumeBuy;
  
  private BigDecimal volumeSell;
  private BigDecimal quoteVolumeSell;
  
  private BigDecimal volumeUnknown;
  private BigDecimal quoteVolumeUnknown;


  private Long totalTrades;
  private Long totalBuyTrades;
  private Long totalSellTrades;
  private Long totalUnknownTrades;

  private LocalDateTime lastUpdated;
}