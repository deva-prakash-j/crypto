package com.crypto.repository;

import com.crypto.entity.OhlcvSyncTracker;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OhlcvSyncTrackerRepository extends JpaRepository<OhlcvSyncTracker, Long> {

    // 🔍 Get all trackers for a given interval (e.g., 1m, 1h, 1d)
    List<OhlcvSyncTracker> findAllByInterval(String interval);

    // 🔍 Get tracker for one pair/ecode/interval
    OhlcvSyncTracker findByPairAndInterval(String pair, String interval);

    // 🔄 Update last synced timestamp after successful fetch
    @Modifying
    @Transactional
    @Query("""
        UPDATE OhlcvSyncTracker 
        SET lastSyncedAt = :lastSyncedAt 
        WHERE pair = :pair AND interval = :interval
    """)
    void updateLastSynced(@Param("pair") String pair,
                          @Param("interval") String interval,
                          @Param("lastSyncedAt") LocalDateTime lastSyncedAt);

    // 🔄 Update firstAvailableAt after initial 1d backfill
    @Modifying
    @Transactional
    @Query("""
        UPDATE OhlcvSyncTracker 
        SET firstAvailableAt = :firstAvailableAt 
        WHERE pair = :pair AND interval = :interval
    """)
    void updateFirstAvailable(@Param("pair") String pair,
                              @Param("interval") String interval,
                              @Param("firstAvailableAt") LocalDateTime firstAvailableAt);

    // 🆕 Upsert (PostgreSQL only): insert or update both timestamps
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO ohlcv_sync_tracker (pair, interval, last_synced_at, first_available_at)
        VALUES (:pair, :interval, :lastSyncedAt, :firstAvailableAt)
        ON CONFLICT (pair, interval) 
        DO UPDATE SET 
          last_synced_at = EXCLUDED.last_synced_at,
          first_available_at = EXCLUDED.first_available_at
        """, nativeQuery = true)
    void upsertTracker(@Param("pair") String pair,
                       @Param("interval") String interval,
                       @Param("lastSyncedAt") LocalDateTime lastSyncedAt,
                       @Param("firstAvailableAt") LocalDateTime firstAvailableAt);
}
