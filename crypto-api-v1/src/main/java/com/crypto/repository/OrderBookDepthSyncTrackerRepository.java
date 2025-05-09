package com.crypto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.crypto.entity.OrderBookDepthSyncTracker;

@Repository
public interface OrderBookDepthSyncTrackerRepository extends JpaRepository<OrderBookDepthSyncTracker, String>{
    
}
