package com.crypto.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crypto.entity.OrderBookDepthSyncTracker;

public interface OrderBookDepthSyncTrackerRepository extends JpaRepository<OrderBookDepthSyncTracker, String>{
    
}
