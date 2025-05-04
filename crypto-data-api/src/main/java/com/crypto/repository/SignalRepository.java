package com.crypto.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.crypto.entity.Signal;
import com.crypto.entity.Signal.SignalOrigin;

public interface SignalRepository extends JpaRepository<Signal, UUID> {

  List<Signal> findByPairAndTimestampBetweenOrderByTimestampDesc(String pair, LocalDateTime from,
      LocalDateTime to);

  boolean existsByTimestampAndPairAndSignalOrigin(LocalDateTime timestamp, String pair,
      SignalOrigin origin);
}
