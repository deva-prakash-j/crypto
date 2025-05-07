package com.crypto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ohlcv_backfill_tracker")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OhlcvBackfillTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "market_type_enum")
    private MarketType marketType;

    private String interval;

    private Long lastSyncedAt;

    private String status; // PENDING, IN_PROGRESS, COMPLETED, ERROR

    public static String generateId(String symbol, MarketType type, String interval) {
        return symbol + "|" + type.name() + "|" + interval;
    }
}
