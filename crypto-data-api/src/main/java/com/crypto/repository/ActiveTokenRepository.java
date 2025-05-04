package com.crypto.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.crypto.entity.ActiveToken;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;

public interface ActiveTokenRepository extends JpaRepository<ActiveToken, UUID> {

  Optional<ActiveToken> findByPair(String pair);
  
  @Query("SELECT t.pair FROM ActiveToken t")
  List<String> findPair();
  
  @Query("SELECT t FROM ActiveToken t WHERE t.backfillDay = false")
  List<ActiveToken> findPairToBackfillDay();

}
