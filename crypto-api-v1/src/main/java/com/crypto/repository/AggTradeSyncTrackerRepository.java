package com.crypto.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crypto.entity.AggTradeSyncTracker;

public interface AggTradeSyncTrackerRepository extends JpaRepository<AggTradeSyncTracker, String> {
    
}
