package com.crypto.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "signals",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"timestamp", "pair", "signalOrigin"})},
    indexes = {@Index(name = "idx_signals_pair_time", columnList = "pair, timestamp DESC"),
        @Index(name = "idx_signals_origin", columnList = "signalOrigin")})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Signal {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  @Column(nullable = false)
  private String pair;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private SignalType signalType; // BUY, SELL, HOLD

  @Column(length = 255)
  private String triggerIndicator;

  @Column(precision = 5, scale = 4)
  private BigDecimal confidenceScore;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private SignalOrigin signalOrigin; // RULE, AI_MODEL

  // Enum definitions (can also go in separate files)
  public enum SignalType {
    BUY, SELL, HOLD
  }

  public enum SignalOrigin {
    RULE, AI_MODEL
  }
}
