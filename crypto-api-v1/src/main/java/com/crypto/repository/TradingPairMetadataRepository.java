package com.crypto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.crypto.entity.MarketType;
import com.crypto.entity.TradingPairMetadata;

import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradingPairMetadataRepository extends JpaRepository<TradingPairMetadata, Long> {

	@Query(value = "SELECT * FROM trading_pair_metadata WHERE symbol = :symbol AND market_type = CAST(:marketType AS market_type_enum)", nativeQuery = true)
	Optional<TradingPairMetadata> findBySymbolAndMarketTypeCast(@Param("symbol") String symbol, @Param("marketType") String marketType);
	
    Optional<TradingPairMetadata> findBySymbolAndMarketType(String symbol, MarketType marketType);

    List<TradingPairMetadata> findByMarketTypeAndIsActiveTrue(MarketType marketType);

}