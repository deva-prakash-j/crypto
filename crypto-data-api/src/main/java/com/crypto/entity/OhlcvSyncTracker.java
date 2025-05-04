package com.crypto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ohlcv_sync_tracker", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"pair", "ecode", "interval"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OhlcvSyncTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pair;

    @Column(nullable = false)
    private String interval; // '1d', '1h', '1m'

    @Column(nullable = false)
    private LocalDateTime lastSyncedAt;

    @Column
    private LocalDateTime firstAvailableAt;
}
