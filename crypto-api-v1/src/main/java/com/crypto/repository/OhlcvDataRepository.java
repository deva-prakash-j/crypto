package com.crypto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.crypto.entity.MarketType;
import com.crypto.entity.OhlcvData;

@Repository
public interface OhlcvDataRepository extends JpaRepository<OhlcvData, Long> {

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