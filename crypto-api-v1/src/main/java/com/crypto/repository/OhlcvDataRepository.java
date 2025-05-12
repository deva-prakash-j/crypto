package com.crypto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.crypto.entity.MarketType;
import com.crypto.entity.OhlcvData;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface OhlcvDataRepository extends JpaRepository<OhlcvData, Long> {

    @Query(value = "SELECT * FROM ohlcv_data WHERE market_type = CAST(:marketType AS market_type_enum) AND symbol = :symbol AND interval = :interval ORDER BY open_time DESC LIMIT 1", nativeQuery = true)
    Optional<OhlcvData> findByLastUpdated(@Param("marketType") String marketType, @Param("symbol") String symbol, @Param("interval") String interval);

    void bulkInsertIgnoreConflicts(List<OhlcvData> dataList);

    boolean existsBySymbolAndMarketTypeAndIntervalAndOpenTime(
        String symbol,
        MarketType marketType,
        String interval,
        Long openTime
    );

    Optional<OhlcvData> findTopBySymbolAndMarketTypeAndIntervalOrderByOpenTimeDesc(
        String symbol,
        MarketType marketType,
        String interval
    );
}