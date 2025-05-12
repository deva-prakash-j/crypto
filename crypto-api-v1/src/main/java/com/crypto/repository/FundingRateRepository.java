package com.crypto.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.crypto.entity.FundingRate;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface FundingRateRepository extends JpaRepository<FundingRate, Long>{
    
    @Query(value = "SELECT * FROM funding_rate WHERE market_type = CAST(:marketType AS market_type_enum) AND symbol = :symbol ORDER BY funding_time DESC LIMIT 1", nativeQuery = true)
    Optional<FundingRate> findByLastUpdated(@Param("marketType") String marketType, @Param("symbol") String symbol);
}
