package com.crypto.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.crypto.entity.SyncTracker;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface SyncTrackerRepository extends JpaRepository<SyncTracker, Long> {

    @Query(value = "SELECT * FROM sync_tracker WHERE market_type = CAST(:marketType AS market_type_enum) AND symbol = :symbol AND type = :type", nativeQuery = true)
    Optional<SyncTracker> findByMarketTypeAndSymbolAndType(@Param("marketType") String marketType, @Param("symbol") String symbol, @Param("type") String type);

    @Query(value = "SELECT * FROM sync_tracker WHERE market_type = CAST(:marketType AS market_type_enum) AND symbol = :symbol AND type = :type AND interval = :interval", nativeQuery = true)
    Optional<SyncTracker> findByMarketTypeAndSymbolAndTypeAndInterval(@Param("marketType") String marketType, @Param("symbol") String symbol, @Param("type") String type, @Param("interval") String interval);
}
