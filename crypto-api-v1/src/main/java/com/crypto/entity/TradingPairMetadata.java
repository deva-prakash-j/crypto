package com.crypto.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "trading_pair_metadata",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"symbol", "marketType"})
    },
    indexes = {
        @Index(name = "idx_market_type", columnList = "marketType"),
        @Index(name = "idx_is_active", columnList = "isActive"),
        @Index(name = "idx_market_type_active", columnList = "marketType, isActive")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingPairMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 20)
    private String baseAsset;

    @Column(nullable = false, length = 20)
    private String quoteAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", columnDefinition = "market_type_enum")
    private MarketType marketType;
    

    @Column(length = 20)
    private String contractType; // Only used for futures

    @Column(length = 20)
    private String marginAsset; // Only used for futures

    private Integer pricePrecision;

    private Integer quantityPrecision;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

