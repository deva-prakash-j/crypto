package com.crypto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.crypto.entity.MarketType;
import com.crypto.entity.TradingPairMetadata;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradingPairMetadataRepository extends JpaRepository<TradingPairMetadata, Long> {

	@Query(value = "SELECT * FROM trading_pair_metadata WHERE symbol = :symbol AND market_type = CAST(:marketType AS market_type_enum)", nativeQuery = true)
	Optional<TradingPairMetadata> findBySymbolAndMarketTypeCast(@Param("symbol") String symbol, @Param("marketType") String marketType);
	
    Optional<TradingPairMetadata> findBySymbolAndMarketType(String symbol, MarketType marketType);

    @Query(value = "SELECT * FROM trading_pair_metadata WHERE market_type = CAST(:marketType AS market_type_enum)", nativeQuery = true)
    List<TradingPairMetadata> findByMarketTypeAndIsActiveTrue(@Param("marketType") String marketType);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM trading_pair_metadata WHERE market_type = CAST(:marketType AS market_type_enum) AND coinalyze_symbol is NULL", nativeQuery = true)
    void deleteUnMappedPairs(@Param("marketType") String marketType);

}