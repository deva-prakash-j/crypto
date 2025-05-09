package com.crypto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.crypto.entity.AggTrade;

@Repository
public interface AggTradeRepository extends JpaRepository<AggTrade, Long>{
    
}
