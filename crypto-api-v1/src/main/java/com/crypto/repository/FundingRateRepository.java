package com.crypto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.crypto.entity.FundingRate;

@Repository
public interface FundingRateRepository extends JpaRepository<FundingRate, Long>{
    
}
