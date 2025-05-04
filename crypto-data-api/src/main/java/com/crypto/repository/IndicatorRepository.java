package com.crypto.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.crypto.entity.Indicator;

public interface IndicatorRepository extends JpaRepository<Indicator, UUID> {

  List<Indicator> findByPairAndIntervalAndTimestampBetweenOrderByTimestampAsc(String pair,
      String interval, LocalDateTime from, LocalDateTime to);

  boolean existsByTimestampAndPairAndInterval(LocalDateTime timestamp, String pair,
      String interval);
}
