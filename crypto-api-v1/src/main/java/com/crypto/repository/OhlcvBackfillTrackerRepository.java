package com.crypto.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.crypto.entity.MarketType;
import com.crypto.entity.OhlcvBackfillTracker;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface OhlcvBackfillTrackerRepository extends JpaRepository<OhlcvBackfillTracker, Long> {

    Optional<OhlcvBackfillTracker> findBySymbolAndIntervalAndMarketType(String symbol, String interval, MarketType marketType);

    	@Query(value = "SELECT * FROM ohlcv_backfill_tracker WHERE symbol = :symbol AND market_type = CAST(:marketType AS market_type_enum) AND interval = :interval", nativeQuery = true)
	Optional<OhlcvBackfillTracker> findBySymbolAndMarketTypeCastAndInterval(@Param("symbol") String symbol, @Param("marketType") String marketType,@Param("interval") String interval);
}

