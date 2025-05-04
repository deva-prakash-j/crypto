package com.crypto.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.crypto.entity.NotificationLog;
import com.crypto.entity.NotificationLog.Channel;
import com.crypto.entity.NotificationLog.Status;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

  List<NotificationLog> findBySignalId(UUID signalId);

  List<NotificationLog> findByChannelAndStatus(Channel channel, Status status);
}
