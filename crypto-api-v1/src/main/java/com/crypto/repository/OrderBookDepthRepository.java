package com.crypto.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crypto.entity.OrderBookDepth;

public interface OrderBookDepthRepository extends JpaRepository<OrderBookDepth, Long>{
    
}
