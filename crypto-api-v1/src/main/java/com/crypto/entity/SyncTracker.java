package com.crypto.entity;

import java.time.LocalDate;

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
@Table(name = "sync_tracker")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncTracker {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "market_type_enum")
    private MarketType marketType;

    private String type;

    private String interval;

    private LocalDate lastSyncedDate;
}
