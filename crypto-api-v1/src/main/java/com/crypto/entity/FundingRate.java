package com.crypto.entity;

import java.math.BigDecimal;

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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "funding_rate")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class FundingRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private BigDecimal fundingRate;
    private Long fundingTime; // Unix millis
    private BigDecimal markPrice;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "market_type_enum")
    private MarketType marketType;
}
