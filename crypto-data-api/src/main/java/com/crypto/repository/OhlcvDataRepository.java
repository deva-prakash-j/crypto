package com.crypto.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.crypto.entity.OhlcvData;

public interface OhlcvDataRepository extends JpaRepository<OhlcvData, UUID> {

  List<OhlcvData> findByPairAndIntervalAndTimestampBetweenOrderByTimestampAsc(String pair,
      String interval, LocalDateTime from, LocalDateTime to);

  boolean existsByTimestampAndPairAndInterval(LocalDateTime timestamp, String pair,
      String interval);
}
