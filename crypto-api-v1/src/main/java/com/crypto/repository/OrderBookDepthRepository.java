package com.crypto.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.crypto.entity.OrderBookDepth;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface OrderBookDepthRepository extends JpaRepository<OrderBookDepth, Long>{
    
    @Query(value = "SELECT * FROM order_book_depth WHERE market_type = CAST(:marketType AS market_type_enum) AND symbol = :symbol ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<OrderBookDepth> findByLastUpdated(@Param("marketType") String marketType, @Param("symbol") String symbol);
}
