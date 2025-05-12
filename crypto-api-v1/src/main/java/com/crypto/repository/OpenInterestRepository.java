package com.crypto.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.crypto.entity.OpenInterest;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface OpenInterestRepository extends JpaRepository<OpenInterest, Long>{

    @Query(value = "SELECT * FROM open_interest WHERE market_type = CAST(:marketType AS market_type_enum) AND symbol = :symbol AND interval = :interval ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<OpenInterest> findByLastUpdated(@Param("marketType") String marketType, @Param("symbol") String symbol, @Param("interval") String interval);
    
}
