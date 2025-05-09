package com.crypto.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "agg_trades")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AggTrade {

    @Id
    @Column(name = "aggregate_trade_id")
    private Long aggregateTradeId;

    private String symbol;

    private BigDecimal price;

    private BigDecimal quantity;

    private Long firstTradeId;

    private Long lastTradeId;

    private Long timestamp;

    private Boolean isBuyerMaker;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "market_type_enum")
    private MarketType marketType;
}
