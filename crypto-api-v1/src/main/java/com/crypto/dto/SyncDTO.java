package com.crypto.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncDTO {

    private LocalDateTime ohlcv1mSyncedAt;
    private LocalDateTime ohlcv1hSyncedAt;
    private LocalDateTime ohlcv1dSyncedAt;
    private LocalDateTime fundingRateSyncedAt;
    private LocalDateTime openInterest5mSyncedAt;
    private LocalDateTime openInterest1hSyncedAt;
    private LocalDateTime openInterest1dSyncedAt;
    private LocalDateTime orderBookSyncedAt;
}
